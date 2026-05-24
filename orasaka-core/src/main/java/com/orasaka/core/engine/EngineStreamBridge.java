package com.orasaka.core.engine;

import com.orasaka.core.pipeline.ContextInterceptor;
import com.orasaka.core.pipeline.OrchestrationPipeline;
import com.orasaka.core.support.ChatCompletedEvent;
import com.orasaka.core.support.InternalChatRequest;
import com.orasaka.core.support.InternalChatResponse;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * Reactive streaming bridge for the Orasaka AI engine.
 *
 * <p>Creates a deferred {@link Flux} of {@link InternalChatResponse} chunks for real-time token
 * streaming. Handles tool execution fallback and post-completion event publishing.
 *
 * <p>The stream lifecycle:
 *
 * <ol>
 *   <li>Compiles the inference context via {@link EnginePipelineBridge#compileContext}
 *   <li>Opens a streaming connection to the resolved {@link
 *       org.springframework.ai.chat.model.ChatModel}
 *   <li>Falls back to tool-stripped streaming on {@link IllegalStateException} (sync or async)
 *   <li>Accumulates all chunks in a {@link StringBuilder} for post-processing
 *   <li>Publishes an {@link ChatCompletedEvent} with the full assembled text on completion
 * </ol>
 *
 * @see AbstractEngine#stream(InternalChatRequest)
 * @see EnginePipelineBridge
 * @since 1.0.0
 */
final class EngineStreamBridge {

  /** Private constructor — utility class, not instantiable. */
  private EngineStreamBridge() {}

  /**
   * Creates a deferred reactive stream of chat response chunks.
   *
   * <p>The {@code Flux.defer()} wrapper ensures that context compilation and model resolution
   * happen lazily at subscription time, not at assembly time.
   *
   * @param request The incoming internal chat request.
   * @param pipeline The orchestration pipeline (nullable — bypassed if null).
   * @param registry The model registry for provider resolution.
   * @param interceptors Ordered interceptors for pre/post-processing.
   * @param eventPublisher Spring event publisher for {@link ChatCompletedEvent}.
   * @return A {@link Flux} emitting incremental {@link InternalChatResponse} chunks.
   */
  static Flux<InternalChatResponse> createStream(
      InternalChatRequest request,
      OrchestrationPipeline pipeline,
      EngineModelRegistry registry,
      List<ContextInterceptor> interceptors,
      ApplicationEventPublisher eventPublisher) {

    return Flux.defer(
        () -> {
          var context =
              EnginePipelineBridge.compileContext(request, pipeline, registry, interceptors);
          var model = registry.getChatModel(context.provider());
          var responseBuilder = new StringBuilder();

          Flux<org.springframework.ai.chat.model.ChatResponse> streamFlux;
          try {
            streamFlux = model.stream(context.toPrompt());
          } catch (IllegalStateException e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EngineStreamBridge.class);
            logger.warn(
                "Model '{}' stream failed tool execution activation, falling back to non-tool text inference stream.",
                context.provider(),
                e);
            streamFlux = model.stream(EnginePipelineBridge.removeTools(context.toPrompt()));
          }

          return streamFlux
              .onErrorResume(
                  IllegalStateException.class,
                  ex -> {
                    org.slf4j.Logger logger =
                        org.slf4j.LoggerFactory.getLogger(EngineStreamBridge.class);
                    logger.warn(
                        "Asynchronous tool execution activation failure, falling back to non-tool text inference stream.",
                        ex);
                    return model.stream(EnginePipelineBridge.removeTools(context.toPrompt()));
                  })
              .map(
                  chatResponse -> {
                    String chunk = chatResponse.getResult().getOutput().getText();
                    if (chunk != null) {
                      responseBuilder.append(chunk);
                    }
                    return new InternalChatResponse(
                        chunk, context.conversationId(), Map.of("provider", context.provider()));
                  })
              .doOnComplete(
                  () -> {
                    String fullText = responseBuilder.toString();
                    interceptors.forEach(
                        i -> i.postProcess(request, context.promptText(), fullText));
                    var out =
                        new InternalChatResponse(
                            fullText,
                            context.conversationId(),
                            Map.of("provider", context.provider()));
                    eventPublisher.publishEvent(new ChatCompletedEvent(request, out));
                  });
        });
  }
}
