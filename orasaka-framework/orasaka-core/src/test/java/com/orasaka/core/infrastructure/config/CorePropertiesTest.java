package com.orasaka.core.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.RoutingMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorePropertiesTest {

  @Test
  void fullConstruction() {
    var rag = new CoreProperties.RagConfig(true, "pgvector", 5);
    var mcp = new CoreProperties.McpConfig(List.of("http://mcp.local"));
    var orchestration =
        new CoreProperties.OrchestrationConfig(
            true,
            new CoreProperties.UserContextConfig(true),
            new CoreProperties.SystemContextConfig(true),
            new CoreProperties.InterceptorConfig(true, "openai", "gpt-4", 0.0),
            new CoreProperties.InterceptorConfig(true, "openai", "gpt-4", 0.0),
            new CoreProperties.RoutingConfig(RoutingMode.AGENTIC, "http://classify.local"));
    var video =
        new CoreProperties.VideoConfig(
            new CoreProperties.VideoAnalysisConfig(10, 2),
            new CoreProperties.VideoGenerationConfig("svd", "http://svd.local"));
    var image =
        new CoreProperties.ImageConfig(
            new CoreProperties.ImageGenerationConfig(
                "openai", "http://img.local", "key", "dall-e-3", 512, 512, 1, 5000, 30000));
    var vision = new CoreProperties.VisionConfig("ollama", "llava");
    var audio = new CoreProperties.AudioConfig("openai", "whisper-1", "whisper-large");

    var props = new CoreProperties("ollama", rag, mcp, orchestration, video, image, vision, audio);
    assertEquals("ollama", props.defaultProvider());
    assertTrue(props.rag().enabled());
    assertEquals("pgvector", props.rag().storeType());
    assertEquals(5, props.rag().topK());
    assertEquals(1, props.mcp().endpoints().size());
    assertTrue(props.orchestration().enabled());
    assertEquals(RoutingMode.AGENTIC, props.orchestration().routing().mode());
  }

  @Test
  void routingConfig_nullMode_defaultsToDeterministic() {
    var routing = new CoreProperties.RoutingConfig(null, null);
    assertEquals(RoutingMode.DETERMINISTIC, routing.mode());
    assertTrue(routing.semanticEndpoint().contains("classify"));
  }

  @Test
  void orchestrationConfig_shortConstructor() {
    var orchestration =
        new CoreProperties.OrchestrationConfig(
            true,
            new CoreProperties.UserContextConfig(false),
            new CoreProperties.SystemContextConfig(false),
            new CoreProperties.InterceptorConfig(false, null, null, null),
            new CoreProperties.InterceptorConfig(false, null, null, null));
    assertNotNull(orchestration.routing());
    assertEquals(RoutingMode.DETERMINISTIC, orchestration.routing().mode());
  }

  @Test
  void audioConfig_shortConstructor() {
    var audio = new CoreProperties.AudioConfig("openai", "whisper-1");
    assertNull(audio.transcriptionModel());
  }
}
