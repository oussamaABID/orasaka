package com.orasaka.gateway.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.PipelineInterceptorConfigEntity;
import org.junit.jupiter.api.Test;

class PipelineInterceptorConfigMapperTest {

  @Test
  void toRecord_mapsAllFields() {
    var entity = new PipelineInterceptorConfigEntity();
    entity.setInterceptorKey("refiner");
    entity.setDisplayLabel("Refiner Interceptor");
    entity.setExecutionOrder(6);
    entity.setIsEnabled(true);
    entity.setDescription("Refines queries into precise instructions");
    InterceptorConfig config = PipelineInterceptorConfigMapper.toRecord(entity);
    assertEquals("refiner", config.interceptorKey());
    assertEquals("Refiner Interceptor", config.displayLabel());
    assertEquals(6, config.executionOrder());
    assertTrue(config.enabled());
    assertEquals("Refines queries into precise instructions", config.description());
  }

  @Test
  void toRecord_nullIsEnabled_mapsFalse() {
    var entity = new PipelineInterceptorConfigEntity();
    entity.setInterceptorKey("router");
    entity.setDisplayLabel("Router");
    entity.setExecutionOrder(7);
    entity.setIsEnabled(null);
    entity.setDescription(null);
    InterceptorConfig config = PipelineInterceptorConfigMapper.toRecord(entity);
    assertFalse(config.enabled());
  }

  @Test
  void toEntity_mapsAllFields() {
    var config = new InterceptorConfig("refiner", "Refiner", 6, true, "desc");
    PipelineInterceptorConfigEntity entity = PipelineInterceptorConfigMapper.toEntity(config);
    assertEquals("refiner", entity.getInterceptorKey());
    assertEquals("Refiner", entity.getDisplayLabel());
    assertEquals(6, entity.getExecutionOrder());
    assertTrue(entity.getIsEnabled());
    assertEquals("desc", entity.getDescription());
  }

  @Test
  void roundTrip_preservesData() {
    var original = new InterceptorConfig("memory", "Memory", 5, false, "Conversation memory");
    PipelineInterceptorConfigEntity entity = PipelineInterceptorConfigMapper.toEntity(original);
    InterceptorConfig roundTripped = PipelineInterceptorConfigMapper.toRecord(entity);
    assertEquals(original, roundTripped);
  }
}
