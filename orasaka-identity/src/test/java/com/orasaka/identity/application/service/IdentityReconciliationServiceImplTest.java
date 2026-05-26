package com.orasaka.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.domain.ports.outbound.AuthorityRepositoryPort;
import com.orasaka.identity.domain.ports.outbound.OAuth2ProviderVerifier;
import com.orasaka.identity.domain.ports.outbound.UserRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

/** Unit tests for {@link IdentityReconciliationServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class IdentityReconciliationServiceImplTest {

  @Mock private OAuth2ProviderVerifier googleVerifier;
  @Mock private UserRepositoryPort userRepository;
  @Mock private AuthorityRepositoryPort authorityRepository;
  @Mock private PlatformTransactionManager transactionManager;

  @Test
  void reconcile_unsupportedProvider_throwsException() {
    // Build service with empty verifiers list
    IdentityReconciliationServiceImpl service =
        new IdentityReconciliationServiceImpl(
            List.of(), userRepository, authorityRepository, transactionManager);

    assertThrows(
        IllegalArgumentException.class, () -> service.reconcile("unknown-provider", "token"));
  }

  @Test
  void constructor_nullVerifiers_initializesEmpty() {
    IdentityReconciliationServiceImpl svc =
        new IdentityReconciliationServiceImpl(
            null, userRepository, authorityRepository, transactionManager);

    assertThrows(IllegalArgumentException.class, () -> svc.reconcile("any-provider", "any-token"));
  }

  @Test
  void constructor_withVerifiers_logsSizeInfo() {
    // Simply ensures no exception on construction with a verifier list
    assertDoesNotThrow(
        () ->
            new IdentityReconciliationServiceImpl(
                List.of(googleVerifier), userRepository, authorityRepository, transactionManager));
  }
}
