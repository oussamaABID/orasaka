package com.orasaka.identity.service;

import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.entity.AuthorityEntity;
import com.orasaka.identity.entity.UserEntity;
import com.orasaka.identity.entity.UserInterceptionEntity;
import com.orasaka.identity.entity.UserInterceptionId;
import com.orasaka.identity.entity.VerificationTokenEntity;
import com.orasaka.identity.event.UserRegisteredEvent;
import com.orasaka.identity.exception.UserAlreadyExistsException;
import com.orasaka.identity.repository.AuthorityRepository;
import com.orasaka.identity.repository.UserInterceptionRepository;
import com.orasaka.identity.repository.UserRepository;
import com.orasaka.identity.repository.VerificationTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Core identity service responsible for user lifecycle management: profile resolution, self-service
 * registration, preference persistence, password-based authentication, user interceptions, and
 * token-based email verification.
 *
 * <p>All database access is performed via Spring Data JPA repositories to prevent SQL injection and
 * ensure codebase purity (AGENTS.md §2.A).
 *
 * <p>Serialization and deserialization are handled automatically at the persistence layer using a
 * JPA converter to keep the service code free of ObjectMapper try-catch noise.
 *
 * @see com.orasaka.identity.domain.User
 */
@Service
public class IdentityService {

  private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final UserInterceptionRepository userInterceptionRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final IdentityInfrastructureProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;

  /**
   * Constructs the service with its JPA repositories, encoding, and event publishing dependencies.
   *
   * @param userRepository The user repository.
   * @param authorityRepository The authority repository.
   * @param verificationTokenRepository The verification token repository.
   * @param userInterceptionRepository The user interception repository.
   * @param passwordEncoder The BCrypt encoder.
   * @param properties The identity infrastructure configurations.
   * @param eventPublisher The application event publisher.
   * @param transactionManager The platform transaction manager.
   */
  public IdentityService(
      UserRepository userRepository,
      AuthorityRepository authorityRepository,
      VerificationTokenRepository verificationTokenRepository,
      UserInterceptionRepository userInterceptionRepository,
      BCryptPasswordEncoder passwordEncoder,
      IdentityInfrastructureProperties properties,
      ApplicationEventPublisher eventPublisher,
      PlatformTransactionManager transactionManager) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.userInterceptionRepository = userInterceptionRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.eventPublisher = eventPublisher;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Resolves a complete {@link User} profile by their unique identifier.
   *
   * @param userId The UUID string identifying the user.
   * @return The resolved {@link User} record, or {@code null} if no user exists.
   */
  public User getUser(String userId) {
    Optional<UserEntity> userOpt = userRepository.findByIdWithAssociations(userId);
    if (userOpt.isEmpty()) {
      return null;
    }
    return mapToUserDomain(userOpt.get());
  }

  /**
   * Merges preferences map into user preferences.
   *
   * @param userId The user UUID string.
   * @param preferences The preference overrides to merge.
   * @return Fully resolved updated {@link User} record.
   */
  public User updatePreferences(String userId, Map<String, Object> preferences) {
    Optional<UserEntity> userOpt = userRepository.findByIdWithAssociations(userId);
    if (userOpt.isEmpty()) {
      logger.warn("updatePreferences: no user found for userId={}", userId);
      return null;
    }

    UserEntity userEntity = userOpt.get();
    Map<String, Object> merged = new HashMap<>(userEntity.getPreferences());
    if (preferences != null) {
      merged.putAll(preferences);
    }

    userEntity.setPreferences(merged);
    userRepository.save(userEntity);
    return mapToUserDomain(userEntity);
  }

  /**
   * Authenticates a user by email and password, filtering out disabled accounts.
   *
   * @param email Plaintext email.
   * @param password Plaintext password.
   * @return Fully resolved {@link User} if successful; {@code null} otherwise.
   */
  public User authenticate(String email, String password) {
    Optional<UserEntity> userOpt = userRepository.findByEmailAndEnabledTrueWithAssociations(email);
    if (userOpt.isEmpty()) {
      return null;
    }

    UserEntity userEntity = userOpt.get();
    if (!passwordEncoder.matches(password, userEntity.getPasswordHash())) {
      return null;
    }

    return mapToUserDomain(userEntity);
  }

  /**
   * JIT provisions or authenticates a user logging in via third-party OAuth2 providers.
   *
   * @param email The social user's email address.
   * @param username The social user's username handle.
   * @return The fully resolved {@link User} record.
   */
  @Transactional
  public User provisionOrAuthenticateOAuth(String email, String username) {
    Optional<UserEntity> userOpt = userRepository.findByEmailAndEnabledTrueWithAssociations(email);
    if (userOpt.isPresent()) {
      return mapToUserDomain(userOpt.get());
    }

    String userId = UUID.randomUUID().toString();
    String passwordHash = "{locked}";

    User validatedUser =
        new User(
            UUID.fromString(userId), username, email, true, Set.of(), Map.of("language", "en"));

    UserEntity userEntity = new UserEntity();
    userEntity.setId(userId);
    userEntity.setUsername(validatedUser.username());
    userEntity.setPasswordHash(passwordHash);
    userEntity.setEmail(validatedUser.email());
    userEntity.setEnabled(true);
    userEntity.setPreferences(validatedUser.preferences());
    userEntity.setRateLimitTier("free");
    userEntity.setCreatedAt(Instant.now());

    userRepository.save(userEntity);

    AuthorityEntity authorityEntity = new AuthorityEntity();
    authorityEntity.setUserId(userId);
    authorityEntity.setAuthorityName("ROLE_USER");
    authorityRepository.save(authorityEntity);

    boolean interceptionsEnabled =
        properties.interceptions() != null && properties.interceptions().enabled();
    List<String> activeInterceptions = List.of();
    if (interceptionsEnabled) {
      triggerInterception(UUID.fromString(userId), "onboarding", "onboarding");
      activeInterceptions = List.of("onboarding");
    }

    logger.info("JIT provisioned new social OAuth2 user: email={}, id={}", email, userId);
    Set<String> authorities = Set.of("ROLE_USER");
    return mapToUserDomain(userEntity, authorities, activeInterceptions);
  }

  /**
   * Registers a new user account, initiating verification or interceptions if configured.
   *
   * @param username Display name.
   * @param email Email address (must be unique).
   * @param password Plaintext password to hash.
   * @param language Initial language preference.
   * @return Fully resolved {@link User} or {@code null} if registration is rejected.
   */
  public User register(String username, String email, String password, String language) {
    String passwordHash = passwordEncoder.encode(password);
    String userId = UUID.randomUUID().toString();

    User validatedUser =
        new User(
            UUID.fromString(userId),
            username,
            email,
            false,
            Set.of(),
            Map.of("language", language != null ? language : ""));

    boolean verificationEnabled =
        properties.emailVerification() != null && properties.emailVerification().enabled();
    boolean enabled = !verificationEnabled;

    String[] plaintextTokenHolder = new String[1];

    User user;
    try {
      user =
          transactionTemplate.execute(
              status -> {
                UserEntity userEntity = new UserEntity();
                userEntity.setId(userId);
                userEntity.setUsername(validatedUser.username());
                userEntity.setPasswordHash(passwordHash);
                userEntity.setEmail(validatedUser.email());
                userEntity.setEnabled(enabled);
                userEntity.setPreferences(validatedUser.preferences());
                userEntity.setCreatedAt(Instant.now());

                userRepository.save(userEntity);

                AuthorityEntity authorityEntity = new AuthorityEntity();
                authorityEntity.setUserId(userId);
                authorityEntity.setAuthorityName("ROLE_USER");
                authorityRepository.save(authorityEntity);

                if (verificationEnabled) {
                  String plaintextToken = UUID.randomUUID().toString();
                  plaintextTokenHolder[0] = plaintextToken;
                  String tokenHash = hashToken(plaintextToken);
                  Instant expiry = Instant.now().plus(24, ChronoUnit.HOURS);

                  VerificationTokenEntity tokenEntity = new VerificationTokenEntity();
                  tokenEntity.setId(UUID.randomUUID().toString());
                  tokenEntity.setUserId(userId);
                  tokenEntity.setTokenType("EMAIL_VERIFICATION");
                  tokenEntity.setTokenHash(tokenHash);
                  tokenEntity.setExpiryTimestamp(expiry);
                  tokenEntity.setUsed(false);
                  tokenEntity.setCreatedAt(Instant.now());

                  verificationTokenRepository.save(tokenEntity);
                  logger.info("Generated verification token for registered user: {}", email);
                }

                boolean interceptionsEnabled =
                    properties.interceptions() != null && properties.interceptions().enabled();
                List<String> activeInterceptions = List.of();
                if (interceptionsEnabled) {
                  triggerInterception(UUID.fromString(userId), "onboarding", "onboarding");
                  activeInterceptions = List.of("onboarding");
                }

                Set<String> authorities = Set.of("ROLE_USER");
                return mapToUserDomain(userEntity, authorities, activeInterceptions);
              });
    } catch (DataIntegrityViolationException ex) {
      logger.warn(
          "Registration rejected — Concurrency safety trigger, email already registered: {}",
          email);
      throw new UserAlreadyExistsException("An account with this email already exists.");
    }

    if (user != null) {
      eventPublisher.publishEvent(new UserRegisteredEvent(user, plaintextTokenHolder[0]));
      logger.debug(
          "New user registered: username={}, id={}, enabled={}",
          validatedUser.username(),
          userId,
          enabled);
    }
    return user;
  }

  /** Maps an UserEntity and its attributes to a clean immutable User domain record in-memory. */
  private User mapToUserDomain(
      UserEntity row, Set<String> authorities, List<String> activeInterceptions) {
    return new User(
        UUID.fromString(row.getId()),
        row.getUsername(),
        row.getEmail(),
        row.getEnabled(),
        authorities,
        row.getPreferences(),
        activeInterceptions,
        row.getRateLimitTier());
  }

  /** Maps user entity with pre-fetched lazy collections to domain User record in-memory. */
  private User mapToUserDomain(UserEntity row) {
    Set<String> authorities =
        row.getAuthorities().stream()
            .map(AuthorityEntity::getAuthorityName)
            .collect(Collectors.toUnmodifiableSet());

    List<String> activeInterceptions =
        row.getInterceptions().stream()
            .map(i -> i.getId().getInterceptionType())
            .collect(Collectors.toList());

    return mapToUserDomain(row, authorities, activeInterceptions);
  }

  /**
   * Triggers an interception block for the specified user.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   * @param schemaId The schema to associate.
   */
  public void triggerInterception(UUID userId, String interceptionType, String schemaId) {
    UserInterceptionId interceptionId = new UserInterceptionId(userId.toString(), interceptionType);
    UserInterceptionEntity entity = new UserInterceptionEntity();
    entity.setId(interceptionId);
    entity.setSchemaId(schemaId);
    entity.setCreatedAt(Instant.now());
    try {
      userInterceptionRepository.saveAndFlush(entity);
      logger.info("Triggered interception '{}' for user {}", interceptionType, userId);
    } catch (DataIntegrityViolationException e) {
      logger.debug("Interception '{}' already exists for user {}", interceptionType, userId);
    }
  }

  /**
   * Resolves an active interception by merging replies and deleting the block row.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   * @param schemaId The configuration schema ID.
   * @param responses Map of user inputs to merge.
   */
  @Transactional
  public void resolveInterception(
      UUID userId, String interceptionType, String schemaId, Map<String, Object> responses) {
    updatePreferences(userId.toString(), responses);

    UserInterceptionId interceptionId = new UserInterceptionId(userId.toString(), interceptionType);
    userInterceptionRepository.deleteById(interceptionId);
    logger.info(
        "Resolved interception '{}' (schema: '{}') for user {}",
        interceptionType,
        schemaId,
        userId);
  }

  /**
   * Verifies a verification token and enables the target user account.
   *
   * @param token Plaintext token.
   * @return True if verified successfully; false otherwise.
   */
  @Transactional
  public boolean verifyToken(String token) {
    String tokenHash = hashToken(token);
    Optional<VerificationTokenEntity> tokenOpt =
        verificationTokenRepository.findByTokenHashAndUsedFalse(tokenHash);
    if (tokenOpt.isEmpty()) {
      logger.warn("Verification token not found or already used");
      return false;
    }

    VerificationTokenEntity row = tokenOpt.get();
    if (row.getExpiryTimestamp() == null || row.getExpiryTimestamp().isBefore(Instant.now())) {
      logger.warn("Verification token has expired");
      return false;
    }

    row.setUsed(true);
    verificationTokenRepository.save(row);

    Optional<UserEntity> userOpt = userRepository.findById(row.getUserId());
    if (userOpt.isPresent()) {
      UserEntity userEntity = userOpt.get();
      userEntity.setEnabled(true);
      userRepository.save(userEntity);
    }

    logger.info("Successfully verified token and enabled user {}", row.getUserId());
    return true;
  }

  /** Computes the SHA-256 hash of a verification token string. */
  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to hash token: SHA-256 digest unavailable", e);
    }
  }
}
