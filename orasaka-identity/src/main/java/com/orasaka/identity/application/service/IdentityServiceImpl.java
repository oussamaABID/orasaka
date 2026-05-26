package com.orasaka.identity.application.service;

import java.security.SecureRandom;
import java.util.Base64;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserCredential;
import com.orasaka.identity.domain.model.UserRegisteredEvent;
import com.orasaka.identity.domain.model.UserSecurityInfo;
import com.orasaka.identity.domain.model.VerificationToken;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.domain.ports.outbound.AuthorityRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.CryptographyPort;
import com.orasaka.identity.domain.ports.outbound.UserCredentialRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.UserInterceptionRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.VerificationTokenRepositoryPort;
import com.orasaka.identity.infrastructure.config.IdentityInfrastructureProperties;
import com.orasaka.identity.infrastructure.support.BadCredentialsException;
import com.orasaka.identity.infrastructure.support.ConfigurationException;
import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import com.orasaka.identity.infrastructure.support.UserAlreadyExistsException;
import com.orasaka.identity.infrastructure.support.UserNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

/**
 * Concrete implementation of the {@link IdentityService} port interface. Decoupled from direct
 * persistence and cryptography frameworks using outbound ports.
 */
@Service
class IdentityServiceImpl implements IdentityService {

  private static final Logger logger = LoggerFactory.getLogger(IdentityServiceImpl.class);
  private static final String ONBOARDING_KEY = "onboarding";
  private static final String INVALID_CREDENTIALS_MSG = "Invalid email or password";
  private static final String ROLE_USER = "ROLE_USER";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepositoryPort userRepository;
  private final AuthorityRepositoryPort authorityRepository;
  private final VerificationTokenRepositoryPort verificationTokenRepository;
  private final UserInterceptionRepositoryPort userInterceptionRepository;
  private final UserCredentialRepositoryPort userCredentialRepository;
  private final CryptographyPort cryptography;
  private final IdentityInfrastructureProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;

  IdentityServiceImpl(
      UserRepositoryPort userRepository,
      AuthorityRepositoryPort authorityRepository,
      VerificationTokenRepositoryPort verificationTokenRepository,
      UserInterceptionRepositoryPort userInterceptionRepository,
      UserCredentialRepositoryPort userCredentialRepository,
      CryptographyPort cryptography,
      IdentityInfrastructureProperties properties,
      ApplicationEventPublisher eventPublisher,
      PlatformTransactionManager transactionManager) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.userInterceptionRepository = userInterceptionRepository;
    this.userCredentialRepository = userCredentialRepository;
    this.cryptography = cryptography;
    this.properties = properties;
    this.eventPublisher = eventPublisher;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUser(String userId) {
    return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
  }

  @Override
  @Transactional
  public User updatePreferences(String userId, Map<String, Object> preferences) {
    return mergeAndSavePreferences(userId, preferences);
  }

  /**
   * Internal helper that merges new preferences into the existing user record and persists the
   * result. Extracted to avoid transactional self-invocation (SonarQube java:S6809).
   */
  private User mergeAndSavePreferences(String userId, Map<String, Object> preferences) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    Map<String, Object> merged = new HashMap<>(user.preferences());
    merged.putAll(preferences != null ? preferences : Map.of());

    User updatedUser =
        new User(
            user.id(),
            user.username(),
            user.email(),
            user.enabled(),
            user.authorities(),
            merged,
            user.activeInterceptions(),
            user.rateLimitTier());

    return userRepository.save(updatedUser);
  }

  @Override
  @Transactional(readOnly = true)
  public User authenticate(String email, String password) {
    Optional<UserSecurityInfo> securityInfoOpt = userRepository.findSecurityInfoByEmail(email);
    if (securityInfoOpt.isEmpty()) {
      throw new BadCredentialsException(INVALID_CREDENTIALS_MSG);
    }

    UserSecurityInfo securityInfo = securityInfoOpt.get();
    User user = securityInfo.user();

    if (securityInfo.passwordHash() == null) {
      logger.debug("Credential authentication rejected for federated user: email={}", email);
      throw new BadCredentialsException(INVALID_CREDENTIALS_MSG);
    }

    if (!cryptography.matchesPassword(password, securityInfo.passwordHash())) {
      throw new BadCredentialsException(INVALID_CREDENTIALS_MSG);
    }

    return user;
  }

  @Override
  public User register(String username, String email, String password, String language) {
    String passwordHash = cryptography.encodePassword(password);
    String userId = UUID.randomUUID().toString();

    Map<String, Object> preferences = new HashMap<>();
    if (language != null) {
      preferences.put("language", language);
    }

    User validatedUser =
        new User(UUID.fromString(userId), username, email, false, Set.of(), preferences);

    boolean verificationEnabled = properties.emailVerification().enabled();
    boolean enabled = !verificationEnabled;

    String[] plaintextTokenHolder = new String[1];

    User user;
    try {
      user =
          transactionTemplate.execute(
              status -> {
                User newUser =
                    new User(
                        validatedUser.id(),
                        validatedUser.username(),
                        validatedUser.email(),
                        enabled,
                        validatedUser.authorities(),
                        validatedUser.preferences(),
                        validatedUser.activeInterceptions(),
                        validatedUser.rateLimitTier());

                User createdUser = userRepository.create(newUser, passwordHash);

                authorityRepository.saveAuthority(userId, ROLE_USER);

                if (verificationEnabled) {
                  byte[] randomBytes = new byte[32];
                  SECURE_RANDOM.nextBytes(randomBytes);
                  String plaintextToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                  plaintextTokenHolder[0] = plaintextToken;
                  String tokenHash = cryptography.hashToken(plaintextToken);
                  Instant expiry = Instant.now().plus(24, ChronoUnit.HOURS);

                  VerificationToken token =
                      new VerificationToken(
                          UUID.randomUUID().toString(),
                          userId,
                          "EMAIL_VERIFICATION",
                          tokenHash,
                          expiry,
                          false);
                  verificationTokenRepository.save(token);
                  logger.info("Generated verification token for registered user: {}", email);
                }

                boolean interceptionsEnabled = properties.interceptions().enabled();
                List<String> activeInterceptions = List.of();
                if (interceptionsEnabled) {
                  userInterceptionRepository.triggerInterception(
                      UUID.fromString(userId), ONBOARDING_KEY, ONBOARDING_KEY);
                  activeInterceptions = List.of(ONBOARDING_KEY);
                }

                Set<String> authorities = Set.of(ROLE_USER);
                return new User(
                    createdUser.id(),
                    createdUser.username(),
                    createdUser.email(),
                    createdUser.enabled(),
                    authorities,
                    createdUser.preferences(),
                    activeInterceptions,
                    createdUser.rateLimitTier());
              });
    } catch (DataIntegrityViolationException ex) {
      logger.warn(
          "Registration rejected — Concurrency safety trigger, email already registered: {}",
          email);
      throw new UserAlreadyExistsException("An account with this email already exists.");
    }

    if (user == null) {
      throw new IllegalStateException("TransactionTemplate returned null user");
    }
    eventPublisher.publishEvent(new UserRegisteredEvent(user, plaintextTokenHolder[0]));
    logger.debug(
        "New user registered: username={}, id={}, enabled={}",
        validatedUser.username(),
        userId,
        enabled);
    return user;
  }

  @Override
  @Transactional
  public void resolveInterception(
      UUID userId, String interceptionType, String schemaId, Map<String, Object> responses) {
    mergeAndSavePreferences(userId.toString(), responses);
    userInterceptionRepository.deleteInterception(userId, interceptionType);
    logger.info(
        "Resolved interception '{}' (schema: '{}') for user {}",
        interceptionType,
        schemaId,
        userId);
  }

  @Override
  @Transactional
  public boolean verifyToken(String token) {
    String tokenHash = cryptography.hashToken(token);
    Optional<VerificationToken> tokenOpt =
        verificationTokenRepository.findByTokenHashAndUsedFalse(tokenHash);
    if (tokenOpt.isEmpty()) {
      logger.warn("Verification token not found or already used");
      return false;
    }

    VerificationToken row = tokenOpt.get();
    if (row.expiryTimestamp() == null || row.expiryTimestamp().isBefore(Instant.now())) {
      logger.warn("Verification token has expired");
      return false;
    }

    verificationTokenRepository.markAsUsed(row.id());

    Optional<User> userOpt = userRepository.findById(row.userId());
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      User updatedUser =
          new User(
              user.id(),
              user.username(),
              user.email(),
              true,
              user.authorities(),
              user.preferences(),
              user.activeInterceptions(),
              user.rateLimitTier());
      userRepository.save(updatedUser);
    }

    logger.info("Successfully verified token and enabled user {}", row.userId());
    return true;
  }

  @Override
  public boolean requiresEmailVerification() {
    return properties.emailVerification().enabled();
  }

  @Override
  public String loadInterceptionSchema(String schemaId) {
    if (properties.interceptions().schemas().isEmpty()) {
      throw new ConfigurationException("Interception schemas not configured; schemaId=" + schemaId);
    }
    Resource resource = properties.interceptions().schemas().get(schemaId);
    if (resource == null || !resource.exists()) {
      throw new ConfigurationException(
          "Configured interception schema file does not exist: " + resource);
    }
    try (InputStream is = resource.getInputStream()) {
      return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to read schema file: {}", resource, e);
      throw new InvalidRequestException("Failed to read interception schema: " + schemaId);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserCredential> getUserCredentials(String userId) {
    return userCredentialRepository.findByUserId(userId);
  }

  @Override
  @Transactional
  public void saveUserCredential(String userId, String providerName, String apiKey) {
    userCredentialRepository.save(userId, providerName, apiKey);
  }

  @Override
  @Transactional
  public void deleteUserCredential(String userId, String providerName) {
    userCredentialRepository.deleteByUserIdAndProviderName(userId, providerName);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> getDecryptedApiKey(String userId, String providerName) {
    return userCredentialRepository.findApiKeyByUserIdAndProviderName(userId, providerName);
  }
}
