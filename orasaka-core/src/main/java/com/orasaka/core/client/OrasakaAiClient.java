package com.orasaka.core.client;

import com.orasaka.core.engine.AbstractOrasakaEngine;
import com.orasaka.core.pipeline.OrasakaKnowledgeService;
import com.orasaka.core.pipeline.OrasakaToolRegistry;
import com.orasaka.core.support.*;
import java.util.List;
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
    InternalChatRequest internalRequest = mapChatRequest(request);
    InternalChatResponse response = engine.chat(internalRequest);
    logger.debug("Core Client chat completed with response: {}", response);
    return mapChatResponse(response);
  }

  public Flux<OrasakaChatResponse> stream(OrasakaChatRequest request) {
    logger.debug("Core Client received streaming chat request: {}", request);
    InternalChatRequest internalRequest = mapChatRequest(request);
    return engine.stream(internalRequest).map(OrasakaAiClient::mapChatResponse);
  }

  public OrasakaImageResponse generateImage(OrasakaImageRequest request) {
    logger.debug("Core Client received image generation request: {}", request);
    InternalImageRequest internalRequest = mapImageRequest(request);
    InternalImageResponse response = engine.generateImage(internalRequest);
    logger.debug("Core Client image generation completed with response: {}", response);
    return mapImageResponse(response);
  }

  public byte[] generateSpeech(OrasakaSpeechRequest request) {
    logger.debug("Core Client received speech generation request: {}", request);
    InternalSpeechRequest internalRequest = mapSpeechRequest(request);
    byte[] response = engine.generateSpeech(internalRequest);
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

  private static OrasakaOptions mapOptions(OrasakaOptions opt) {
    return opt;
  }

  private static OrasakaContext mapContext(OrasakaContext ctx) {
    return ctx;
  }

  private static InternalChatRequest.ChatMessage mapChatMessage(
      OrasakaChatRequest.ChatMessage msg) {
    if (msg == null) {
      return null;
    }
    return new InternalChatRequest.ChatMessage(msg.role(), msg.content());
  }

  private static InternalChatRequest mapChatRequest(OrasakaChatRequest req) {
    if (req == null) {
      return null;
    }
    List<InternalChatRequest.ChatMessage> mappedMessages = List.of();
    if (req.messages() != null) {
      mappedMessages = req.messages().stream().map(OrasakaAiClient::mapChatMessage).toList();
    }
    return new InternalChatRequest(
        req.prompt(), mappedMessages, mapOptions(req.options()), mapContext(req.context()));
  }

  private static OrasakaChatResponse mapChatResponse(InternalChatResponse resp) {
    if (resp == null) {
      return null;
    }
    return new OrasakaChatResponse(resp.content(), resp.conversationId(), resp.metadata());
  }

  private static InternalImageRequest mapImageRequest(OrasakaImageRequest req) {
    if (req == null) {
      return null;
    }
    return new InternalImageRequest(
        req.prompt(),
        req.width(),
        req.height(),
        mapOptions(req.options()),
        mapContext(req.context()));
  }

  private static OrasakaImageResponse mapImageResponse(InternalImageResponse resp) {
    if (resp == null) {
      return null;
    }
    return new OrasakaImageResponse(resp.imageData(), resp.url(), resp.format());
  }

  private static InternalSpeechRequest mapSpeechRequest(OrasakaSpeechRequest req) {
    if (req == null) {
      return null;
    }
    return new InternalSpeechRequest(
        req.text(), mapOptions(req.options()), mapContext(req.context()));
  }
}
