package com.orasaka.core.client;

import com.orasaka.core.context.OrasakaContext;
import com.orasaka.core.engine.AbstractOrasakaEngine;
import com.orasaka.core.identity.OrasakaAuthority;
import com.orasaka.core.model.*;
import com.orasaka.core.pipeline.OrasakaKnowledgeService;
import com.orasaka.core.pipeline.OrasakaToolRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * High-level Facade for developers to interact with the Orasaka AI Ecosystem.
 *
 * <p>This client serves as the primary entry point for all AI interactions, including multi-modal
 * chat, image generation, and knowledge retrieval. It maps clean, Spring-AI-agnostic domain models
 * into internal models containing framework-dependent types.
 */
@Component
public class OrasakaAiClient {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaAiClient.class);

  private final AbstractOrasakaEngine engine;
  private final OrasakaToolRegistry toolRegistry;
  private final OrasakaKnowledgeService knowledgeService;

  public OrasakaAiClient(
      AbstractOrasakaEngine engine,
      OrasakaToolRegistry toolRegistry,
      OrasakaKnowledgeService knowledgeService) {
    this.engine = engine;
    this.toolRegistry = toolRegistry;
    this.knowledgeService = knowledgeService;
  }

  public OrasakaChatResponse chat(OrasakaChatRequest request) {
    logger.debug("Core Client received chat request: {}", request);
    com.orasaka.core.support.OrasakaChatRequest supportRequest = mapChatRequest(request);
    com.orasaka.core.support.OrasakaChatResponse response = engine.chat(supportRequest);
    logger.debug("Core Client chat completed with response: {}", response);
    return mapChatResponse(response);
  }

  public Flux<OrasakaChatResponse> stream(OrasakaChatRequest request) {
    logger.debug("Core Client received streaming chat request: {}", request);
    com.orasaka.core.support.OrasakaChatRequest supportRequest = mapChatRequest(request);
    return engine.stream(supportRequest).map(OrasakaAiClient::mapChatResponse);
  }

  public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
    logger.debug("Core Client received image generation request: {}", request);
    com.orasaka.core.support.OrasakaImageRequest supportRequest = mapImageRequest(request);
    com.orasaka.core.support.OrasakaImageResponse response = engine.generateImage(supportRequest);
    logger.debug("Core Client image generation completed with response: {}", response);
    return mapImageResponse(response);
  }

  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    logger.debug("Core Client received speech generation request: {}", request);
    com.orasaka.core.support.OrasakaSpeechRequest supportRequest = mapSpeechRequest(request);
    byte[] response = engine.generateSpeech(supportRequest);
    logger.debug(
        "Core Client speech generation completed with {} bytes",
        response != null ? response.length : 0);
    return response;
  }

  public OrasakaToolRegistry getToolRegistry() {
    return toolRegistry;
  }

  public OrasakaKnowledgeService getKnowledgeService() {
    return knowledgeService;
  }

  // --- Mapping Helper Methods ---

  private static com.orasaka.core.support.OrasakaOptions mapOptions(OrasakaOptions opt) {
    if (opt == null) {
      return null;
    }
    return new com.orasaka.core.support.OrasakaOptions() {
      @Override
      public Double getTemperature() {
        return opt.getTemperature();
      }

      @Override
      public Integer getMaxTokens() {
        return opt.getMaxTokens();
      }

      @Override
      public Map<String, Object> getExtraOptions() {
        return opt.getExtraOptions();
      }

      @Override
      public com.orasaka.core.support.OrasakaOptions withOption(String key, Object value) {
        return mapOptions(opt.withOption(key, value));
      }
    };
  }

  private static com.orasaka.core.support.OrasakaContext mapContext(OrasakaContext ctx) {
    if (ctx == null) {
      return null;
    }
    Set<String> roles =
        ctx.authorities().stream().map(OrasakaAuthority::name).collect(Collectors.toSet());
    return new com.orasaka.core.support.OrasakaContext(
        ctx.userId(), ctx.conversationId(), ctx.preferences(), roles);
  }

  private static com.orasaka.core.support.OrasakaChatRequest.ChatMessage mapChatMessage(
      OrasakaChatRequest.ChatMessage msg) {
    if (msg == null) {
      return null;
    }
    return new com.orasaka.core.support.OrasakaChatRequest.ChatMessage(msg.role(), msg.content());
  }

  private static com.orasaka.core.support.OrasakaChatRequest mapChatRequest(
      OrasakaChatRequest req) {
    if (req == null) {
      return null;
    }
    List<com.orasaka.core.support.OrasakaChatRequest.ChatMessage> mappedMessages = List.of();
    if (req.messages() != null) {
      mappedMessages = req.messages().stream().map(OrasakaAiClient::mapChatMessage).toList();
    }
    return new com.orasaka.core.support.OrasakaChatRequest(
        req.prompt(), mappedMessages, mapOptions(req.options()), mapContext(req.context()));
  }

  private static OrasakaChatResponse mapChatResponse(
      com.orasaka.core.support.OrasakaChatResponse resp) {
    if (resp == null) {
      return null;
    }
    return new OrasakaChatResponse(resp.content(), resp.conversationId(), resp.metadata());
  }

  private static com.orasaka.core.support.OrasakaImageRequest mapImageRequest(
      OrasakaImageRequest req) {
    if (req == null) {
      return null;
    }
    return new com.orasaka.core.support.OrasakaImageRequest(
        req.prompt(),
        req.width(),
        req.height(),
        mapOptions(req.options()),
        mapContext(req.context()));
  }

  private static OrasakaImageResponse mapImageResponse(
      com.orasaka.core.support.OrasakaImageResponse resp) {
    if (resp == null) {
      return null;
    }
    return new OrasakaImageResponse(resp.imageData(), resp.url(), resp.format());
  }

  private static com.orasaka.core.support.OrasakaSpeechRequest mapSpeechRequest(
      OrasakaSpeechRequest req) {
    if (req == null) {
      return null;
    }
    return new com.orasaka.core.support.OrasakaSpeechRequest(
        req.text(), mapOptions(req.options()), mapContext(req.context()));
  }
}
