package com.orasaka.core.engine;

import com.orasaka.core.pipeline.OrasakaContextInterceptor;
import com.orasaka.core.pipeline.OrasakaOrchestrationPipeline;
import com.orasaka.core.support.OrasakaChatCompletedEvent;
import com.orasaka.core.support.OrasakaChatRequest;
import com.orasaka.core.support.OrasakaChatResponse;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

final class EngineStreamBridge {

  private EngineStreamBridge() {}

  static Flux<OrasakaChatResponse> createStream(
      OrasakaChatRequest request,
      OrasakaOrchestrationPipeline pipeline,
      EngineModelRegistry registry,
      List<OrasakaContextInterceptor> interceptors,
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
                    return new OrasakaChatResponse(
                        chunk, context.conversationId(), Map.of("provider", context.provider()));
                  })
              .doOnComplete(
                  () -> {
                    String fullText = responseBuilder.toString();
                    interceptors.forEach(
                        i -> i.postProcess(request, context.promptText(), fullText));
                    var out =
                        new OrasakaChatResponse(
                            fullText,
                            context.conversationId(),
                            Map.of("provider", context.provider()));
                    eventPublisher.publishEvent(new OrasakaChatCompletedEvent(request, out));
                  });
        });
  }
}
