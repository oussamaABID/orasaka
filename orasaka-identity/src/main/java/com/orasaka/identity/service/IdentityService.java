package com.orasaka.identity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.identity.OrasakaAuthority;
import com.orasaka.identity.config.IdentityInfrastructureProperties;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.entity.OrasakaAuthorityEntity;
import com.orasaka.identity.entity.OrasakaUserEntity;
import com.orasaka.identity.entity.OrasakaUserInterceptionEntity;
import com.orasaka.identity.entity.OrasakaUserInterceptionId;
import com.orasaka.identity.entity.OrasakaVerificationTokenEntity;
import com.orasaka.identity.event.UserRegisteredEvent;
import com.orasaka.identity.repository.OrasakaAuthorityRepository;
import com.orasaka.identity.repository.OrasakaUserInterceptionRepository;
import com.orasaka.identity.repository.OrasakaUserRepository;
import com.orasaka.identity.repository.OrasakaVerificationTokenRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core identity service responsible for user lifecycle management: profile resolution, self-service
 * registration, preference persistence, password-based authentication, user interceptions, and
 * token-based email verification.
 *
 * <p>All database access is performed via Spring Data JPA repositories to prevent SQL injection and
 * ensure codebase purity (AGENTS.md §2.A).
 *
 * <p>JSON preference columns are parsed and serialized through private utility helpers to keep core
 * business methods free of {@code ObjectMapper} try-catch noise.
 *
 * @see com.orasaka.identity.domain.User
 * @see com.orasaka.core.identity.OrasakaAuthority
 */
@Service
public class IdentityService {

  private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

  /** Default language used when registration omits the language field. */
  private static final String DEFAULT_LANGUAGE = "en";

  private final OrasakaUserRepository userRepository;
  private final OrasakaAuthorityRepository authorityRepository;
  private final OrasakaVerificationTokenRepository verificationTokenRepository;
  private final OrasakaUserInterceptionRepository userInterceptionRepository;
  private final ObjectMapper jsonMapper;
  private final BCryptPasswordEncoder passwordEncoder;
  private final IdentityInfrastructureProperties properties;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs the service with its JPA repositories, encoding, and event publishing dependencies.
   *
   * @param userRepository The user repository.
   * @param authorityRepository The authority repository.
   * @param verificationTokenRepository The verification token repository.
   * @param userInterceptionRepository The user interception repository.
   * @param objectMapper The Jackson mapper.
   * @param passwordEncoder The BCrypt encoder.
   * @param properties The identity infrastructure configurations.
   * @param eventPublisher The application event publisher.
   */
  public IdentityService(
      OrasakaUserRepository userRepository,
      OrasakaAuthorityRepository authorityRepository,
      OrasakaVerificationTokenRepository verificationTokenRepository,
      OrasakaUserInterceptionRepository userInterceptionRepository,
      ObjectMapper jsonMapper,
      BCryptPasswordEncoder passwordEncoder,
      IdentityInfrastructureProperties properties,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.userInterceptionRepository = userInterceptionRepository;
    this.jsonMapper = jsonMapper;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.eventPublisher = eventPublisher;
  }

  // ─── Public API ──────────────────────────────────────────────────────────

  /**
   * Resolves a complete {@link User} profile by their unique identifier.
   *
   * @param userId The UUID string identifying the user.
   * @return The resolved {@link User} record, or {@code null} if no user exists.
   */
  public User getUser(String userId) {
    Optional<OrasakaUserEntity> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      return null;
    }
    return mapToUserDomainWithAssociations(userOpt.get());
  }

  /**
   * Merges preferences map into user preferences.
   *
   * @param userId The user UUID string.
   * @param preferences The preference overrides to merge.
   * @return Fully resolved updated {@link User} record.
   */
  public User updatePreferences(String userId, Map<String, Object> preferences) {
    Optional<OrasakaUserEntity> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      logger.warn("updatePreferences: no user found for userId={}", userId);
      return null;
    }

    OrasakaUserEntity userEntity = userOpt.get();
    Map<String, Object> merged = new HashMap<>(parsePreferences(userEntity.getPreferences()));
    if (preferences != null) {
      merged.putAll(preferences);
    }

    userEntity.setPreferences(serializePreferences(merged));
    userRepository.save(userEntity);
    return mapToUserDomainWithAssociations(userEntity);
  }

  /**
   * Authenticates a user by email and password, filtering out disabled accounts.
   *
   * @param email Plaintext email.
   * @param password Plaintext password.
   * @return Fully resolved {@link User} if successful; {@code null} otherwise.
   */
  public User authenticate(String email, String password) {
    Optional<OrasakaUserEntity> userOpt = userRepository.findByEmailAndEnabledTrue(email);
    if (userOpt.isEmpty()) {
      return null;
    }

    OrasakaUserEntity userEntity = userOpt.get();
    if (!passwordEncoder.matches(password, userEntity.getPasswordHash())) {
      return null;
    }

    return mapToUserDomainWithAssociations(userEntity);
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
    Optional<OrasakaUserEntity> userOpt = userRepository.findByEmailAndEnabledTrue(email);
    if (userOpt.isPresent()) {
      return mapToUserDomainWithAssociations(userOpt.get());
    }

    String userId = UUID.randomUUID().toString();
    String passwordHash = "{locked}";
    String initialPreferences = serializePreferences(Map.of("language", DEFAULT_LANGUAGE));

    OrasakaUserEntity userEntity = new OrasakaUserEntity();
    userEntity.setId(userId);
    userEntity.setUsername(username);
    userEntity.setPasswordHash(passwordHash);
    userEntity.setEmail(email);
    userEntity.setEnabled(true);
    userEntity.setPreferences(initialPreferences);
    userEntity.setRateLimitTier("free");
    userEntity.setCreatedAt(Instant.now());

    userRepository.save(userEntity);

    OrasakaAuthorityEntity authorityEntity = new OrasakaAuthorityEntity();
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
    Set<OrasakaAuthority> authorities = Set.of(new OrasakaAuthority("ROLE_USER"));
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
  @Transactional
  public User register(String username, String email, String password, String language) {
    long count = userRepository.countByEmail(email);
    if (count > 0) {
      logger.warn("Registration rejected — email already registered: {}", email);
      return null;
    }

    String userId = UUID.randomUUID().toString();
    String passwordHash = passwordEncoder.encode(password);

    String lang = (language != null && !language.isBlank()) ? language : DEFAULT_LANGUAGE;
    String initialPreferences = serializePreferences(Map.of("language", lang));

    boolean verificationEnabled =
        properties.emailVerification() != null && properties.emailVerification().enabled();
    boolean enabled = !verificationEnabled;

    OrasakaUserEntity userEntity = new OrasakaUserEntity();
    userEntity.setId(userId);
    userEntity.setUsername(username);
    userEntity.setPasswordHash(passwordHash);
    userEntity.setEmail(email);
    userEntity.setEnabled(enabled);
    userEntity.setPreferences(initialPreferences);
    userEntity.setCreatedAt(Instant.now());

    userRepository.save(userEntity);

    OrasakaAuthorityEntity authorityEntity = new OrasakaAuthorityEntity();
    authorityEntity.setUserId(userId);
    authorityEntity.setAuthorityName("ROLE_USER");
    authorityRepository.save(authorityEntity);

    String plaintextToken = null;
    if (verificationEnabled) {
      plaintextToken = UUID.randomUUID().toString();
      String tokenHash = hashToken(plaintextToken);
      Instant expiry = Instant.now().plus(24, ChronoUnit.HOURS);

      OrasakaVerificationTokenEntity tokenEntity = new OrasakaVerificationTokenEntity();
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

    Set<OrasakaAuthority> authorities = Set.of(new OrasakaAuthority("ROLE_USER"));
    User user = mapToUserDomain(userEntity, authorities, activeInterceptions);
    eventPublisher.publishEvent(new UserRegisteredEvent(user, plaintextToken));
    logger.debug("New user registered: username={}, id={}, enabled={}", username, userId, enabled);
    return user;
  }

  /**
   * Maps an OrasakaUserEntity and its attributes to a clean immutable User domain record in-memory.
   */
  private User mapToUserDomain(
      OrasakaUserEntity row, Set<OrasakaAuthority> authorities, List<String> activeInterceptions) {
    Map<String, Object> preferences = parsePreferences(row.getPreferences());
    return new User(
        UUID.fromString(row.getId()),
        row.getUsername(),
        row.getEmail(),
        row.getEnabled(),
        authorities,
        preferences,
        activeInterceptions,
        row.getRateLimitTier());
  }

  /** Maps user entity and loads its permissions and interceptions. */
  private User mapToUserDomainWithAssociations(OrasakaUserEntity row) {
    String userId = row.getId();
    Set<OrasakaAuthority> authorities =
        authorityRepository.findByUserId(userId).stream()
            .map(a -> new OrasakaAuthority(a.getAuthorityName()))
            .collect(Collectors.toUnmodifiableSet());

    List<String> activeInterceptions =
        userInterceptionRepository.findByIdUserId(userId).stream()
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
    OrasakaUserInterceptionId interceptionId =
        new OrasakaUserInterceptionId(userId.toString(), interceptionType);
    if (!userInterceptionRepository.existsById(interceptionId)) {
      OrasakaUserInterceptionEntity entity = new OrasakaUserInterceptionEntity();
      entity.setId(interceptionId);
      entity.setSchemaId(schemaId);
      entity.setCreatedAt(Instant.now());
      userInterceptionRepository.save(entity);
      logger.info("Triggered interception '{}' for user {}", interceptionType, userId);
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

    OrasakaUserInterceptionId interceptionId =
        new OrasakaUserInterceptionId(userId.toString(), interceptionType);
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
    Optional<OrasakaVerificationTokenEntity> tokenOpt =
        verificationTokenRepository.findByTokenHashAndUsedFalse(tokenHash);
    if (tokenOpt.isEmpty()) {
      logger.warn("Verification token not found or already used");
      return false;
    }

    OrasakaVerificationTokenEntity row = tokenOpt.get();
    if (row.getExpiryTimestamp() == null || row.getExpiryTimestamp().isBefore(Instant.now())) {
      logger.warn("Verification token has expired");
      return false;
    }

    row.setUsed(true);
    verificationTokenRepository.save(row);

    Optional<OrasakaUserEntity> userOpt = userRepository.findById(row.getUserId());
    if (userOpt.isPresent()) {
      OrasakaUserEntity userEntity = userOpt.get();
      userEntity.setEnabled(true);
      userRepository.save(userEntity);
    }

    logger.info("Successfully verified token and enabled user {}", row.getUserId());
    return true;
  }

  // ─── Private Helpers ─────────────────────────────────────────────────────

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

  /** Parses user preferences JSON string to Map. */
  private Map<String, Object> parsePreferences(String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    try {
      return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      logger.warn(
          "Failed to parse user preferences JSON — returning empty map. Cause: {}", e.getMessage());
      return new HashMap<>();
    }
  }

  /** Serializes user preferences Map to JSON. */
  private String serializePreferences(Map<String, Object> preferences) {
    try {
      return jsonMapper.writeValueAsString(preferences);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize user preferences to JSON", e);
    }
  }
}
