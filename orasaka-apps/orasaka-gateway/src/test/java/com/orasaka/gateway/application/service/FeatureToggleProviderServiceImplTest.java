package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.persistence.infrastructure.adapter.persistence.entity.FeatureFlagEntity;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.FeatureFlagRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FeatureToggleProviderServiceImplTest {

  private final FeatureFlagRepository repository = mock(FeatureFlagRepository.class);
  private final FeatureToggleProviderServiceImpl service =
      new FeatureToggleProviderServiceImpl(repository);

  @Test
  void isEnabled_returnsTrue_whenFeatureEnabled() {
    var entity = new FeatureFlagEntity();
    entity.setFeatureKey("chat");
    entity.setIsEnabled(true);
    when(repository.findById("chat")).thenReturn(Optional.of(entity));
    Optional<Boolean> result = service.isEnabled("chat");
    assertTrue(result.isPresent());
    assertTrue(result.get());
  }

  @Test
  void isEnabled_returnsFalse_whenFeatureDisabled() {
    var entity = new FeatureFlagEntity();
    entity.setFeatureKey("video");
    entity.setIsEnabled(false);
    when(repository.findById("video")).thenReturn(Optional.of(entity));
    Optional<Boolean> result = service.isEnabled("video");
    assertTrue(result.isPresent());
    assertFalse(result.get());
  }

  @Test
  void isEnabled_returnsEmpty_whenFeatureNotFound() {
    when(repository.findById("unknown")).thenReturn(Optional.empty());
    Optional<Boolean> result = service.isEnabled("unknown");
    assertTrue(result.isEmpty());
  }

  @Test
  void isEnabled_nullKey_throws() {
    assertThrows(NullPointerException.class, () -> service.isEnabled(null));
  }

  @Test
  void constructor_nullRepository_throws() {
    assertThrows(NullPointerException.class, () -> new FeatureToggleProviderServiceImpl(null));
  }
}
