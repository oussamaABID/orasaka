package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.ports.inbound.IdentityService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserCredentialsProviderImplTest {

  private final IdentityService identityService = mock(IdentityService.class);
  private final UserCredentialsProviderImpl provider =
      new UserCredentialsProviderImpl(identityService);

  @Test
  void getDecryptedApiKey_delegatesToIdentityService() {
    when(identityService.getDecryptedApiKey("user-1", "openai"))
        .thenReturn(Optional.of("sk-test-key"));
    Optional<String> result = provider.getDecryptedApiKey("user-1", "openai");
    assertTrue(result.isPresent());
    assertEquals("sk-test-key", result.get());
  }

  @Test
  void getDecryptedApiKey_returnsEmpty_whenNotFound() {
    when(identityService.getDecryptedApiKey("user-1", "unknown")).thenReturn(Optional.empty());
    Optional<String> result = provider.getDecryptedApiKey("user-1", "unknown");
    assertTrue(result.isEmpty());
  }

  @Test
  void getDecryptedApiKey_nullUserId_returnsEmpty() {
    Optional<String> result = provider.getDecryptedApiKey(null, "openai");
    assertTrue(result.isEmpty());
    verifyNoInteractions(identityService);
  }

  @Test
  void getDecryptedApiKey_nullProviderName_returnsEmpty() {
    Optional<String> result = provider.getDecryptedApiKey("user-1", null);
    assertTrue(result.isEmpty());
    verifyNoInteractions(identityService);
  }

  @Test
  void constructor_nullIdentityService_throws() {
    assertThrows(NullPointerException.class, () -> new UserCredentialsProviderImpl(null));
  }
}
