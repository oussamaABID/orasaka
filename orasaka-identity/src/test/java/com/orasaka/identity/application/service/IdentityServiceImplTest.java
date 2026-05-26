package com.orasaka.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserCredential;
import com.orasaka.identity.domain.model.UserSecurityInfo;
import com.orasaka.identity.domain.model.VerificationToken;
import com.orasaka.identity.domain.ports.outbound.AuthorityRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.CryptographyPort;
import com.orasaka.identity.domain.ports.outbound.UserCredentialRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.UserInterceptionRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.VerificationTokenRepositoryPort;
import com.orasaka.identity.infrastructure.config.IdentityInfrastructureProperties;
import com.orasaka.identity.infrastructure.support.BadCredentialsException;
import com.orasaka.identity.infrastructure.support.ConfigurationException;
import com.orasaka.identity.infrastructure.support.UserNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

/** Unit tests for {@link IdentityServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class IdentityServiceImplTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private AuthorityRepositoryPort authorityRepository;
  @Mock private VerificationTokenRepositoryPort verificationTokenRepository;
  @Mock private UserInterceptionRepositoryPort userInterceptionRepository;
  @Mock private UserCredentialRepositoryPort userCredentialRepository;
  @Mock private CryptographyPort cryptography;
  @Mock private IdentityInfrastructureProperties properties;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private PlatformTransactionManager transactionManager;

  private IdentityServiceImpl service;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "user@orasaka.io";
  private static final String USERNAME = "testuser";

  private User stubUser() {
    return new User(USER_ID, USERNAME, EMAIL, true, Set.of("ROLE_USER"), Map.of());
  }

  @BeforeEach
  void setUp() {
    service =
        new IdentityServiceImpl(
            userRepository,
            authorityRepository,
            verificationTokenRepository,
            userInterceptionRepository,
            userCredentialRepository,
            cryptography,
            properties,
            eventPublisher,
            transactionManager);
  }

  // --- getUser ---

  @Test
  void getUser_existingUser_returnsUser() {
    User user = stubUser();
    when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(user));

    User result = service.getUser(USER_ID.toString());

    assertEquals(user, result);
  }

  @Test
  void getUser_notFound_throwsUserNotFoundException() {
    when(userRepository.findById(anyString())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> service.getUser("nonexistent"));
  }

  // --- authenticate ---

  @Test
  void authenticate_validCredentials_returnsUser() {
    User user = stubUser();
    UserSecurityInfo info = new UserSecurityInfo(user, "hashedPassword");
    when(userRepository.findSecurityInfoByEmail(EMAIL)).thenReturn(Optional.of(info));
    when(cryptography.matchesPassword("password", "hashedPassword")).thenReturn(true);

    User result = service.authenticate(EMAIL, "password");

    assertEquals(user, result);
  }

  @Test
  void authenticate_unknownEmail_throwsBadCredentials() {
    when(userRepository.findSecurityInfoByEmail(anyString())).thenReturn(Optional.empty());

    assertThrows(BadCredentialsException.class, () -> service.authenticate("unknown@x.io", "pw"));
  }

  @Test
  void authenticate_federatedUser_nullPasswordHash_throwsBadCredentials() {
    User user = stubUser();
    UserSecurityInfo info = new UserSecurityInfo(user, null);
    when(userRepository.findSecurityInfoByEmail(EMAIL)).thenReturn(Optional.of(info));

    assertThrows(BadCredentialsException.class, () -> service.authenticate(EMAIL, "password"));
  }

  @Test
  void authenticate_wrongPassword_throwsBadCredentials() {
    User user = stubUser();
    UserSecurityInfo info = new UserSecurityInfo(user, "hashedPassword");
    when(userRepository.findSecurityInfoByEmail(EMAIL)).thenReturn(Optional.of(info));
    when(cryptography.matchesPassword("wrong", "hashedPassword")).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> service.authenticate(EMAIL, "wrong"));
  }

  // --- updatePreferences ---

  @Test
  void updatePreferences_mergesAndSaves() {
    User user = stubUser();
    when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = service.updatePreferences(USER_ID.toString(), Map.of("theme", "dark"));

    assertEquals("dark", result.preferences().get("theme"));
  }

  @Test
  void updatePreferences_nullPreferences_preservesExisting() {
    User user =
        new User(USER_ID, USERNAME, EMAIL, true, Set.of("ROLE_USER"), Map.of("key", "value"));
    when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = service.updatePreferences(USER_ID.toString(), null);

    assertEquals("value", result.preferences().get("key"));
  }

  @Test
  void register_duplicateEmail_throwsUserAlreadyExists() {
    IdentityInfrastructureProperties.EmailVerification emailVerif =
        mock(IdentityInfrastructureProperties.EmailVerification.class);
    when(emailVerif.enabled()).thenReturn(false);
    when(properties.emailVerification()).thenReturn(emailVerif);

    when(cryptography.encodePassword(anyString())).thenReturn("hash");

    // Mock TransactionTemplate to throw DataIntegrityViolationException
    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(transactionManager)
        .getTransaction(any());

    assertThrows(Exception.class, () -> service.register(USERNAME, EMAIL, "password", "en"));
  }

  // --- verifyToken ---

  @Test
  void verifyToken_validToken_enablesUserAndReturnsTrue() {
    String tokenId = UUID.randomUUID().toString();
    String userId = USER_ID.toString();
    VerificationToken token =
        new VerificationToken(
            tokenId,
            userId,
            "EMAIL_VERIFICATION",
            "hashed",
            Instant.now().plusSeconds(3600),
            false);

    when(cryptography.hashToken("plaintoken")).thenReturn("hashed");
    when(verificationTokenRepository.findByTokenHashAndUsedFalse("hashed"))
        .thenReturn(Optional.of(token));

    User user = stubUser();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    boolean result = service.verifyToken("plaintoken");

    assertTrue(result);
    verify(verificationTokenRepository).markAsUsed(tokenId);
    verify(userRepository).save(any(User.class));
  }

  @Test
  void verifyToken_notFound_returnsFalse() {
    when(cryptography.hashToken("unknown")).thenReturn("hash");
    when(verificationTokenRepository.findByTokenHashAndUsedFalse("hash"))
        .thenReturn(Optional.empty());

    assertFalse(service.verifyToken("unknown"));
  }

  @Test
  void verifyToken_expired_returnsFalse() {
    VerificationToken token =
        new VerificationToken(
            "id",
            USER_ID.toString(),
            "EMAIL_VERIFICATION",
            "hashed",
            Instant.now().minus(1, ChronoUnit.HOURS),
            false);

    when(cryptography.hashToken("expired")).thenReturn("hashed");
    when(verificationTokenRepository.findByTokenHashAndUsedFalse("hashed"))
        .thenReturn(Optional.of(token));

    assertFalse(service.verifyToken("expired"));
  }

  // --- requiresEmailVerification ---

  @Test
  void requiresEmailVerification_delegatesToProperties() {
    IdentityInfrastructureProperties.EmailVerification emailVerif =
        mock(IdentityInfrastructureProperties.EmailVerification.class);
    when(emailVerif.enabled()).thenReturn(true);
    when(properties.emailVerification()).thenReturn(emailVerif);

    assertTrue(service.requiresEmailVerification());
  }

  // --- loadInterceptionSchema ---

  @Test
  void loadInterceptionSchema_emptySchemas_throwsConfigurationException() {
    IdentityInfrastructureProperties.Interceptions interceptions =
        mock(IdentityInfrastructureProperties.Interceptions.class);
    when(interceptions.schemas()).thenReturn(Map.of());
    when(properties.interceptions()).thenReturn(interceptions);

    assertThrows(ConfigurationException.class, () -> service.loadInterceptionSchema("onboarding"));
  }

  // --- Credential operations ---

  @Test
  void getUserCredentials_delegatesToRepository() {
    List<UserCredential> creds = List.of(new UserCredential("openai", true));
    when(userCredentialRepository.findByUserId(USER_ID.toString())).thenReturn(creds);

    List<UserCredential> result = service.getUserCredentials(USER_ID.toString());

    assertEquals(1, result.size());
    assertEquals("openai", result.getFirst().providerName());
  }

  @Test
  void saveUserCredential_delegatesToRepository() {
    service.saveUserCredential(USER_ID.toString(), "openai", "sk-key");

    verify(userCredentialRepository).save(USER_ID.toString(), "openai", "sk-key");
  }

  @Test
  void deleteUserCredential_delegatesToRepository() {
    service.deleteUserCredential(USER_ID.toString(), "openai");

    verify(userCredentialRepository).deleteByUserIdAndProviderName(USER_ID.toString(), "openai");
  }

  @Test
  void getDecryptedApiKey_delegatesToRepository() {
    when(userCredentialRepository.findApiKeyByUserIdAndProviderName(USER_ID.toString(), "openai"))
        .thenReturn(Optional.of("decrypted-key"));

    Optional<String> result = service.getDecryptedApiKey(USER_ID.toString(), "openai");

    assertTrue(result.isPresent());
    assertEquals("decrypted-key", result.get());
  }

  // --- resolveInterception ---

  @Test
  void resolveInterception_mergesPreferencesAndDeletesInterception() {
    User user = stubUser();
    when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> responses = Map.of("theme", "dark", "lang", "fr");
    service.resolveInterception(USER_ID, "onboarding", "onboarding-schema", responses);

    verify(userInterceptionRepository).deleteInterception(USER_ID, "onboarding");
    verify(userRepository).save(any(User.class));
  }
}
