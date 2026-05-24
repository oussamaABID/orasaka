package com.orasaka.core.client;

import com.orasaka.core.engine.AbstractEngine;
import com.orasaka.core.pipeline.KnowledgeService;
import com.orasaka.core.pipeline.ToolRegistry;
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
public class AiClient {

  private static final Logger logger = LoggerFactory.getLogger(AiClient.class);

  private final AbstractEngine engine;
  private final ToolRegistry toolRegistry;
  private final KnowledgeService knowledgeService;

  /**
   * Constructs the AI client facade.
   *
   * @param engine The abstract AI engine for inference operations.
   * @param toolRegistry The tool registry for tool-augmented generation.
   * @param knowledgeService The knowledge service for RAG operations.
   */
  public AiClient(
      AbstractEngine engine, ToolRegistry toolRegistry, KnowledgeService knowledgeService) {
    this.engine = engine;
    this.toolRegistry = toolRegistry;
    this.knowledgeService = knowledgeService;
  }

  /**
   * Executes a synchronous chat completion.
   *
   * @param request The public chat request with prompt, history, options, and context.
   * @return The chat response with generated content and metadata.
   */
  public ChatResponse chat(ChatRequest request) {
    logger.debug("Core Client received chat request: {}", request);
    InternalChatRequest internalRequest = mapChatRequest(request);
    InternalChatResponse response = engine.chat(internalRequest);
    logger.debug("Core Client chat completed with response: {}", response);
    return mapChatResponse(response);
  }

  /**
   * Creates a reactive token stream for real-time chat completion.
   *
   * @param request The public chat request.
   * @return A {@link Flux} emitting incremental {@link ChatResponse} chunks.
   */
  public Flux<ChatResponse> stream(ChatRequest request) {
    logger.debug("Core Client received streaming chat request: {}", request);
    InternalChatRequest internalRequest = mapChatRequest(request);
    return engine.stream(internalRequest).map(AiClient::mapChatResponse);
  }

  /**
   * Generates an image from a text prompt.
   *
   * @param request The public image generation request with prompt and dimensions.
   * @return The image response with raw bytes and/or URL.
   */
  public ImageResponse generateImage(ImageRequest request) {
    logger.debug("Core Client received image generation request: {}", request);
    InternalImageRequest internalRequest = mapImageRequest(request);
    InternalImageResponse response = engine.generateImage(internalRequest);
    logger.debug("Core Client image generation completed with response: {}", response);
    return mapImageResponse(response);
  }

  /**
   * Generates speech audio from text.
   *
   * @param request The public speech request with text and voice preferences.
   * @return The raw audio bytes (MP3 format).
   */
  public byte[] generateSpeech(SpeechRequest request) {
    logger.debug("Core Client received speech generation request: {}", request);
    InternalSpeechRequest internalRequest = mapSpeechRequest(request);
    byte[] response = engine.generateSpeech(internalRequest);
    logger.debug(
        "Core Client speech generation completed with {} bytes",
        response != null ? response.length : 0);
    return response;
  }

  /** Returns the registered tool registry for tool-augmented generation. */
  public ToolRegistry getToolRegistry() {
    return toolRegistry;
  }

  /** Returns the knowledge service for RAG-based context retrieval. */
  public KnowledgeService getKnowledgeService() {
    return knowledgeService;
  }

  // --- Mapping Helper Methods ---

  private static Options mapOptions(Options opt) {
    return opt;
  }

  private static Context mapContext(Context ctx) {
    return ctx;
  }

  private static InternalChatRequest.ChatMessage mapChatMessage(ChatRequest.ChatMessage msg) {
    if (msg == null) {
      return null;
    }
    return new InternalChatRequest.ChatMessage(msg.role(), msg.content());
  }

  private static InternalChatRequest mapChatRequest(ChatRequest req) {
    if (req == null) {
      return null;
    }
    List<InternalChatRequest.ChatMessage> mappedMessages = List.of();
    if (req.messages() != null) {
      mappedMessages = req.messages().stream().map(AiClient::mapChatMessage).toList();
    }
    return new InternalChatRequest(
        req.prompt(), mappedMessages, mapOptions(req.options()), mapContext(req.context()));
  }

  private static ChatResponse mapChatResponse(InternalChatResponse resp) {
    if (resp == null) {
      return null;
    }
    return new ChatResponse(resp.content(), resp.conversationId(), resp.metadata());
  }

  private static InternalImageRequest mapImageRequest(ImageRequest req) {
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

  private static ImageResponse mapImageResponse(InternalImageResponse resp) {
    if (resp == null) {
      return null;
    }
    return new ImageResponse(resp.imageData(), resp.url(), resp.format());
  }

  private static InternalSpeechRequest mapSpeechRequest(SpeechRequest req) {
    if (req == null) {
      return null;
    }
    return new InternalSpeechRequest(
        req.text(), mapOptions(req.options()), mapContext(req.context()));
  }
}
