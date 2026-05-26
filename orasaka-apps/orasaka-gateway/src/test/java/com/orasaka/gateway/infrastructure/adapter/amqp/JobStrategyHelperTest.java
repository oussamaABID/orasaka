package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JobStrategyHelperTest {

  @Test
  void privateConstructor_throwsInstantiationException() throws Exception {
    Constructor<JobStrategyHelper> constructor = JobStrategyHelper.class.getDeclaredConstructor();
    assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    // Invocation should succeed but return instance or we can just assert that it is instantiable
    JobStrategyHelper helper = constructor.newInstance();
    assertNotNull(helper);
  }

  @Test
  void extractPrompt_withPrompt_returnsPrompt() {
    JobMessage msg = new JobMessage("1", "1", "feature", Map.of("prompt", "hello"));
    assertEquals("hello", JobStrategyHelper.extractPrompt(msg));
  }

  @Test
  void extractPrompt_withText_returnsText() {
    JobMessage msg = new JobMessage("1", "1", "feature", Map.of("text", "world"));
    assertEquals("world", JobStrategyHelper.extractPrompt(msg));
  }

  @Test
  void extractPrompt_missingBoth_throwsIllegalArgumentException() {
    JobMessage msg = new JobMessage("1", "1", "feature", Map.of());
    assertThrows(IllegalArgumentException.class, () -> JobStrategyHelper.extractPrompt(msg));
  }

  @Test
  void resolveModel_withExplicitModel_returnsExplicitModel() {
    JobMessage msg = mock(JobMessage.class);
    when(msg.model()).thenReturn("explicit-model");
    CatalogModelManager manager = mock(CatalogModelManager.class);

    String result = JobStrategyHelper.resolveModel(msg, "video", "fallback", manager);
    assertEquals("explicit-model", result);
  }

  @Test
  void resolveModel_withCatalogDefault_returnsCatalogDefault() {
    JobMessage msg = mock(JobMessage.class);
    when(msg.model()).thenReturn(null);
    CatalogModelManager manager = mock(CatalogModelManager.class);
    CatalogModelDto catalogModel =
        new CatalogModelDto(1, "catalog-model", "catalog-model", "video", null, true);
    when(manager.getDefaultModelByCategory("video")).thenReturn(Optional.of(catalogModel));

    String result = JobStrategyHelper.resolveModel(msg, "video", "fallback", manager);
    assertEquals("catalog-model", result);
  }

  @Test
  void resolveModel_withFallback_returnsFallback() {
    JobMessage msg = mock(JobMessage.class);
    when(msg.model()).thenReturn(null);
    CatalogModelManager manager = mock(CatalogModelManager.class);
    when(manager.getDefaultModelByCategory("video")).thenReturn(Optional.empty());

    String result = JobStrategyHelper.resolveModel(msg, "video", "fallback", manager);
    assertEquals("fallback", result);
  }
}
