package com.orasaka.identity.service;

import com.orasaka.identity.domain.ExtractedProfile;
import com.orasaka.identity.domain.User;
import com.orasaka.identity.entity.AuthorityEntity;
import com.orasaka.identity.entity.UserEntity;
import com.orasaka.identity.federation.OAuth2ProviderVerifier;
import com.orasaka.identity.repository.AuthorityRepository;
import com.orasaka.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package-private concrete implementation of the {@link IdentityReconciliationService} port
 * interface.
 *
 * <p>Aggregates all active {@link OAuth2ProviderVerifier} strategy beans (conditionally loaded
 * based on configuration flags) and performs identity verification and reconciliation.
 *
 * <p><strong>Architectural invariants:</strong>
 * <ul>
 *   <li>No read-before-write pattern — race conditions handled via
 *       {@link DataIntegrityViolationException} (per naming_conventions.md §4).</li>
 *   <li>In-memory domain mapping — no redundant database reads (per ERR-200).</li>
 *   <li>Stateless verification — no session state maintained (per ERR-105).</li>
 * </ul>
 *
 * @see IdentityReconciliationService
 * @see OAuth2ProviderVerifier
 * @see ExtractedProfile
 */
@Service
class IdentityReconciliationServiceImpl implements IdentityReconciliationService {

  private static final Logger logger =
      LoggerFactory.getLogger(IdentityReconciliationServiceImpl.class);

  private final List<OAuth2ProviderVerifier> verifiers;
  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;

  /**
   * Constructs the reconciliation service with all active provider verifiers and repositories.
   *
   * @param verifiers The auto-aggregated list of active {@link OAuth2ProviderVerifier} beans.
   *     May be empty if no providers are enabled.
   * @param userRepository The user repository for persistence operations.
   * @param authorityRepository The authority repository for role assignment.
   */
  IdentityReconciliationServiceImpl(
      List<OAuth2ProviderVerifier> verifiers,
      UserRepository userRepository,
      AuthorityRepository authorityRepository) {
    this.verifiers = verifiers != null ? List.copyOf(verifiers) : List.of();
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    logger.info(
        "Identity reconciliation service initialized with {} active provider verifier(s)",
        this.verifiers.size());
  }

  @Override
  @Transactional
  public User reconcile(String provider, String idToken) {
    OAuth2ProviderVerifier verifier = resolveVerifier(provider);
    ExtractedProfile profile = verifier.verifyAndExtract(idToken);

    logger.debug(
        "Token verified for provider={}, extractedEmail={}", provider, profile.email());

    Optional<UserEntity> existingUser =
        userRepository.findByProviderAndProviderId(provider, profile.providerId());

    if (existingUser.isPresent()) {
      logger.debug("Existing federated user found for provider={}, providerId={}",
          provider, profile.providerId());
      return mapToUserDomain(existingUser.get());
    }

    return provisionFederatedUser(provider, profile);
  }

  /**
   * Provisions a new federated user in the database with no password hash.
   *
   * <p>Handles race conditions via {@link DataIntegrityViolationException} from the
   * {@code unique_provider_user} database constraint rather than read-before-write checks.
   */
  private User provisionFederatedUser(String provider, ExtractedProfile profile) {
    String userId = UUID.randomUUID().toString();

    UserEntity userEntity = UserMapper.toEntity(profile, userId, provider, "free");

    try {
      userRepository.save(userEntity);
    } catch (DataIntegrityViolationException ex) {
      logger.warn(
          "Concurrent federated user creation detected for provider={}, providerId={}. "
              + "Falling back to lookup.",
          provider, profile.providerId());
      return userRepository
          .findByProviderAndProviderId(provider, profile.providerId())
          .map(this::mapToUserDomain)
          .orElseThrow(() -> new IllegalStateException(
              "Failed to provision or resolve federated user for provider=" + provider));
    }

    AuthorityEntity authorityEntity = new AuthorityEntity();
    authorityEntity.setUserId(userId);
    authorityEntity.setAuthorityName("ROLE_USER");
    authorityRepository.save(authorityEntity);

    logger.info(
        "Provisioned new federated user: provider={}, email={}, id={}",
        provider, profile.email(), userId);

    Set<String> authorities = Set.of("ROLE_USER");
    return new User(
        UUID.fromString(userId),
        userEntity.getUsername(),
        userEntity.getEmail(),
        true,
        authorities,
        userEntity.getPreferences(),
        List.of(),
        userEntity.getRateLimitTier());
  }

  /** Resolves the matching provider verifier or throws if none supports the provider. */
  private OAuth2ProviderVerifier resolveVerifier(String provider) {
    return verifiers.stream()
        .filter(v -> v.supports(provider))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No active verifier found for provider: "
                        + provider
                        + ". Ensure the provider is enabled in configuration."));
  }

  /** Maps a user entity with pre-fetched lazy collections to an immutable domain User record. */
  private User mapToUserDomain(UserEntity row) {
    Set<String> authorities =
        row.getAuthorities().stream()
            .map(AuthorityEntity::getAuthorityName)
            .collect(Collectors.toUnmodifiableSet());

    List<String> activeInterceptions =
        row.getInterceptions().stream()
            .map(i -> i.getId().getInterceptionType())
            .collect(Collectors.toList());

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
}
