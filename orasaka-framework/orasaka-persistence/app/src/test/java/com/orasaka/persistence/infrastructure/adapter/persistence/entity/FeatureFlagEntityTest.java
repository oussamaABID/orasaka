package com.orasaka.persistence.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FeatureFlagEntityTest {

  @Test
  void setAndGetFeatureKey() {
    var entity = new FeatureFlagEntity();
    entity.setFeatureKey("chat_generation");
    assertEquals("chat_generation", entity.getFeatureKey());
  }

  @Test
  void setAndGetIsEnabled() {
    var entity = new FeatureFlagEntity();
    entity.setIsEnabled(true);
    assertTrue(entity.getIsEnabled());
  }

  @Test
  void defaultValues_areNull() {
    var entity = new FeatureFlagEntity();
    assertNull(entity.getFeatureKey());
    assertNull(entity.getIsEnabled());
  }
}
