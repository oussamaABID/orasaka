package com.orasaka.core.engine;

import com.orasaka.core.pipeline.OrasakaContextInterceptor;
import com.orasaka.core.pipeline.OrasakaOrchestrationPipeline;
import com.orasaka.core.pipeline.PromptContext;
import com.orasaka.core.support.OrasakaChatRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

record EnginePipelineContext(
    String provider, String conversationId, String promptText, Prompt toPrompt) {}

final class EnginePipelineBridge {

  private EnginePipelineBridge() {}

  static EnginePipelineContext compileContext(
      OrasakaChatRequest request,
      OrasakaOrchestrationPipeline pipeline,
      EngineModelRegistry registry,
      List<OrasakaContextInterceptor> interceptors) {

    PromptContext pipelineContext =
        Optional.ofNullable(pipeline)
            .map(p -> p.process(request.prompt(), request.context()))
            .orElse(null);

    String promptText =
        Optional.ofNullable(pipelineContext)
            .map(PromptContext::refinedPrompt)
            .orElseGet(request::prompt);

    String provider =
        Optional.ofNullable(pipelineContext)
            .map(PromptContext::routedProvider)
            .orElseGet(registry::getActiveProvider);

    List<Message> messages = request.compileMessages(promptText, EnginePipelineBridge::mapMessage);
    ChatOptions springOptions = OrasakaOptionsMapper.mapOptions(request.options(), provider);

    ChatOptions finalOptions =
        interceptors.stream()
            .reduce(
                springOptions,
                (opts, interceptor) -> interceptor.preProcess(request, promptText, messages, opts),
                (o1, o2) -> o1);

    Prompt prompt = new Prompt(messages, finalOptions);
    String conversationId =
        Optional.ofNullable(request.context())
            .map(com.orasaka.core.support.OrasakaContext::conversationId)
            .orElse(null);

    return new EnginePipelineContext(provider, conversationId, promptText, prompt);
  }

  static Message mapMessage(OrasakaChatRequest.ChatMessage msg) {
    return switch (msg.role().toLowerCase()) {
      case "system" -> new SystemMessage(msg.content());
      case "assistant" -> new AssistantMessage(msg.content());
      default -> new UserMessage(msg.content());
    };
  }
}
