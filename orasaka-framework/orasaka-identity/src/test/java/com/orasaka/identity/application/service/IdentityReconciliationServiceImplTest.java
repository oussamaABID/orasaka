package com.orasaka.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.model.ExtractedProfile;
import com.orasaka.identity.domain.model.Persona;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.outbound.AuthorityRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.OAuth2ProviderVerifier;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

/** Unit tests for {@link IdentityReconciliationServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class IdentityReconciliationServiceImplTest {

  @Mock private OAuth2ProviderVerifier googleVerifier;
  @Mock private UserRepositoryPort userRepository;
  @Mock private AuthorityRepositoryPort authorityRepository;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private TransactionStatus transactionStatus;

  private IdentityReconciliationServiceImpl service;

  @BeforeEach
  void setUp() {
    lenient().when(googleVerifier.supports("google")).thenReturn(true);
    service =
        new IdentityReconciliationServiceImpl(
            List.of(googleVerifier), userRepository, authorityRepository, transactionManager);
  }

  @Test
  void reconcile_unsupportedProvider_throwsException() {
    IdentityReconciliationServiceImpl emptyService =
        new IdentityReconciliationServiceImpl(
            List.of(), userRepository, authorityRepository, transactionManager);

    assertThrows(IllegalArgumentException.class, () -> emptyService.reconcile("google", "token"));
  }

  @Test
  void constructor_nullVerifiers_initializesEmpty() {
    IdentityReconciliationServiceImpl nullService =
        new IdentityReconciliationServiceImpl(
            null, userRepository, authorityRepository, transactionManager);

    assertThrows(IllegalArgumentException.class, () -> nullService.reconcile("google", "token"));
  }

  @Test
  void constructor_withVerifiers_logsSizeInfo() {
    assertDoesNotThrow(
        () ->
            new IdentityReconciliationServiceImpl(
                List.of(googleVerifier), userRepository, authorityRepository, transactionManager));
  }

  @Test
  void reconcile_existingUser_returnsExistingUser() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User existingUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.of(existingUser));

    User result = service.reconcile("google", "id-token");

    assertEquals(existingUser, result);
    verify(userRepository, never()).create(any(), any());
    verify(authorityRepository, never()).saveAuthority(any(), any());
  }

  @Test
  void reconcile_newUser_provisionsAndReturnsSavedUser() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User savedUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.create(any(User.class), eq(null))).thenReturn(savedUser);

    User result = service.reconcile("google", "id-token");

    assertEquals(savedUser, result);
    verify(userRepository).create(any(User.class), eq(null));
    verify(authorityRepository).saveAuthority(anyString(), eq("ROLE_USER"));
  }

  @Test
  void reconcile_newUserConcurrentCreation_fallsBackToLookup() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User existingUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty()) // first lookup: not found
        .thenReturn(Optional.of(existingUser)); // fallback lookup: found
    when(userRepository.create(any(User.class), eq(null)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    User result = service.reconcile("google", "id-token");

    assertEquals(existingUser, result);
    verify(authorityRepository, never()).saveAuthority(any(), any());
  }

  @Test
  void reconcile_newUserConcurrentCreationFallbackFails_throwsException() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.create(any(User.class), eq(null)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThrows(IllegalStateException.class, () -> service.reconcile("google", "id-token"));
  }
}
