package com.orasaka.identity.application.service;

import com.orasaka.identity.domain.model.ExtractedProfile;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityReconciliationService;
import com.orasaka.identity.domain.ports.outbound.AuthorityRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.OAuth2ProviderVerifier;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Package-private concrete implementation of the {@link IdentityReconciliationService} port
 * interface.
 */
@Service
class IdentityReconciliationServiceImpl implements IdentityReconciliationService {

  private static final Logger logger =
      LoggerFactory.getLogger(IdentityReconciliationServiceImpl.class);

  private final List<OAuth2ProviderVerifier> verifiers;
  private final UserRepositoryPort userRepository;
  private final AuthorityRepositoryPort authorityRepository;
  private final TransactionTemplate transactionTemplate;

  IdentityReconciliationServiceImpl(
      List<OAuth2ProviderVerifier> verifiers,
      UserRepositoryPort userRepository,
      AuthorityRepositoryPort authorityRepository,
      PlatformTransactionManager transactionManager) {
    this.verifiers = verifiers != null ? List.copyOf(verifiers) : List.of();
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    logger.info(
        "Identity reconciliation service initialized with {} active provider verifier(s)",
        this.verifiers.size());
  }

  @Override
  public User reconcile(String provider, String idToken) {
    OAuth2ProviderVerifier verifier = resolveVerifier(provider);
    ExtractedProfile profile = verifier.verifyAndExtract(idToken);

    logger.debug("Token verified for provider={}, extractedEmail={}", provider, profile.email());

    return transactionTemplate.execute(
        status -> {
          Optional<User> existingUser =
              userRepository.findByProviderAndProviderId(provider, profile.providerId());

          if (existingUser.isPresent()) {
            logger.debug(
                "Existing federated user found for provider={}, providerId={}",
                provider,
                profile.providerId());
            return existingUser.get();
          }

          return provisionFederatedUser(provider, profile);
        });
  }

  private User provisionFederatedUser(String provider, ExtractedProfile profile) {
    String userId = UUID.randomUUID().toString();

    User newUser =
        new User(
            UUID.fromString(userId),
            profile.name(),
            profile.email(),
            true,
            Set.of("ROLE_USER"),
            Map.of("language", "en"),
            List.of(),
            "free");

    User savedUser;
    try {
      savedUser = userRepository.create(newUser, null);
    } catch (DataIntegrityViolationException ex) {
      logger.warn(
          "Concurrent federated user creation detected for provider={}, providerId={}. "
              + "Falling back to lookup.",
          provider,
          profile.providerId());
      return userRepository
          .findByProviderAndProviderId(provider, profile.providerId())
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Failed to provision or resolve federated user for provider=" + provider));
    }

    authorityRepository.saveAuthority(userId, "ROLE_USER");

    logger.info(
        "Provisioned new federated user: provider={}, email={}, id={}",
        provider,
        profile.email(),
        userId);

    return savedUser;
  }

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
}
