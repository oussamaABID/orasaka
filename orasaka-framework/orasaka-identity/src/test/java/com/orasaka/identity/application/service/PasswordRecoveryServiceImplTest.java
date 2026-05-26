package com.orasaka.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.model.PasswordResetRequestedEvent;
import com.orasaka.identity.domain.model.PasswordResetToken;
import com.orasaka.identity.domain.model.Persona;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.outbound.CryptographyPort;
import com.orasaka.identity.domain.ports.outbound.PasswordEventPublisher;
import com.orasaka.identity.domain.ports.outbound.PasswordResetTokenRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link PasswordRecoveryServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceImplTest {

  @Mock private PasswordResetTokenRepositoryPort tokenRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private CryptographyPort cryptography;
  @Mock private PasswordEventPublisher passwordEventPublisher;

  private PasswordRecoveryServiceImpl service;

  private static final String EMAIL = Persona.freeUser().email();

  private User stubUser() {
    return Persona.freeUser();
  }

  @BeforeEach
  void setUp() {
    service =
        new PasswordRecoveryServiceImpl(
            tokenRepository, userRepository, cryptography, passwordEventPublisher);
  }

  // --- requestPasswordReset ---

  @Test
  void requestPasswordReset_existingUser_savesTokenAndPublishesEvent() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(stubUser()));
    when(cryptography.hashToken(anyString())).thenReturn("sha256hexhash1234567890");

    service.requestPasswordReset(EMAIL);

    verify(tokenRepository).deleteByEmail(EMAIL);
    verify(tokenRepository).save(any(PasswordResetToken.class));

    ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor =
        ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
    verify(passwordEventPublisher).publish(eventCaptor.capture());

    PasswordResetRequestedEvent event = eventCaptor.getValue();
    assertEquals(EMAIL, event.email());
    assertNotNull(event.plaintextToken());
  }

  @Test
  void requestPasswordReset_nonExistentUser_silentlyReturns() {
    when(userRepository.findByEmail("unknown@x.io")).thenReturn(Optional.empty());

    service.requestPasswordReset("unknown@x.io");

    verify(tokenRepository, never()).save(any());
    verify(passwordEventPublisher, never()).publish(any());
  }

  @Test
  void requestPasswordReset_nullEmail_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.requestPasswordReset(null));
  }

  @Test
  void requestPasswordReset_purgesExistingTokensBeforeSaving() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(stubUser()));
    when(cryptography.hashToken(anyString())).thenReturn("hash123456789012");

    service.requestPasswordReset(EMAIL);

    var inOrder = inOrder(tokenRepository);
    inOrder.verify(tokenRepository).deleteByEmail(EMAIL);
    inOrder.verify(tokenRepository).save(any(PasswordResetToken.class));
  }

  @Test
  void requestPasswordReset_savedTokenHasCorrectFields() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(stubUser()));
    when(cryptography.hashToken(anyString())).thenReturn("computed_hash_value_abcd");

    service.requestPasswordReset(EMAIL);

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());

    PasswordResetToken saved = tokenCaptor.getValue();
    assertEquals(EMAIL, saved.email());
    assertEquals("computed_hash_value_abcd", saved.tokenHash());
    assertNotNull(saved.id());
    assertTrue(saved.expiresAt().isAfter(Instant.now()));
  }

  // --- resetPassword ---

  @Test
  void resetPassword_validToken_updatesPasswordAndDeletesToken() {
    String tokenId = UUID.randomUUID().toString();
    PasswordResetToken token =
        new PasswordResetToken(
            tokenId, EMAIL, "tokenHash", Instant.now().plus(10, ChronoUnit.MINUTES));

    when(cryptography.hashToken("plaintext-token")).thenReturn("tokenHash");
    when(tokenRepository.findByTokenHash("tokenHash")).thenReturn(Optional.of(token));
    when(cryptography.encodePassword("NewPassword123")).thenReturn("bcrypt_encoded");

    service.resetPassword("plaintext-token", "NewPassword123");

    verify(userRepository).updatePasswordHashByEmail(EMAIL, "bcrypt_encoded");
    verify(tokenRepository).deleteById(tokenId);
  }

  @Test
  void resetPassword_expiredToken_throwsInvalidRequest() {
    String tokenId = UUID.randomUUID().toString();
    PasswordResetToken expiredToken =
        new PasswordResetToken(
            tokenId, EMAIL, "expiredHash", Instant.now().minus(1, ChronoUnit.HOURS));

    when(cryptography.hashToken("expired-token")).thenReturn("expiredHash");
    when(tokenRepository.findByTokenHash("expiredHash")).thenReturn(Optional.of(expiredToken));

    InvalidRequestException ex =
        assertThrows(
            InvalidRequestException.class,
            () -> service.resetPassword("expired-token", "NewPassword123"));

    assertEquals("Invalid or expired reset token", ex.getMessage());
    verify(tokenRepository).deleteById(tokenId);
    verify(userRepository, never()).updatePasswordHashByEmail(anyString(), anyString());
  }

  @Test
  void resetPassword_invalidToken_throwsInvalidRequest() {
    when(cryptography.hashToken("bad-token")).thenReturn("unknownHash");
    when(tokenRepository.findByTokenHash("unknownHash")).thenReturn(Optional.empty());

    InvalidRequestException ex =
        assertThrows(
            InvalidRequestException.class,
            () -> service.resetPassword("bad-token", "NewPassword123"));

    assertEquals("Invalid or expired reset token", ex.getMessage());
  }

  @Test
  void resetPassword_shortPassword_throwsInvalidRequest() {
    InvalidRequestException ex =
        assertThrows(
            InvalidRequestException.class, () -> service.resetPassword("any-token", "short"));

    assertEquals("Password must be at least 8 characters", ex.getMessage());
    verify(tokenRepository, never()).findByTokenHash(anyString());
  }

  @Test
  void resetPassword_nullToken_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.resetPassword(null, "NewPassword123"));
  }

  @Test
  void resetPassword_nullPassword_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.resetPassword("some-token", null));
  }

  @Test
  void resetPassword_bcryptExecutedBeforeDbUpdate() {
    String tokenId = UUID.randomUUID().toString();
    PasswordResetToken token =
        new PasswordResetToken(tokenId, EMAIL, "hash", Instant.now().plus(10, ChronoUnit.MINUTES));

    when(cryptography.hashToken("token")).thenReturn("hash");
    when(tokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
    when(cryptography.encodePassword("ValidPass123")).thenReturn("bcrypt_result");

    service.resetPassword("token", "ValidPass123");

    var inOrder = inOrder(cryptography, userRepository, tokenRepository);
    inOrder.verify(cryptography).encodePassword("ValidPass123");
    inOrder.verify(userRepository).updatePasswordHashByEmail(EMAIL, "bcrypt_result");
    inOrder.verify(tokenRepository).deleteById(tokenId);
  }
}
