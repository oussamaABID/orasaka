package com.orasaka.core.engine;

import com.orasaka.core.pipeline.ContextInterceptor;
import com.orasaka.core.pipeline.OrchestrationPipeline;
import com.orasaka.core.pipeline.PromptContext;
import com.orasaka.core.support.InternalChatRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Static bridge utility that compiles a complete inference context from a raw chat request.
 *
 * <p>This class orchestrates the pre-inference pipeline:
 *
 * <ol>
 *   <li>Runs the {@link OrchestrationPipeline} (refiner → router → context resolvers)
 *   <li>Resolves the target provider from the pipeline or falls back to the registry default
 *   <li>Compiles chat messages from the request's message history
 *   <li>Maps Orasaka options to Spring AI {@link ChatOptions}
 *   <li>Applies all registered {@link ContextInterceptor} pre-processors
 * </ol>
 *
 * <p>Also provides the {@link #removeTools} utility for stripping tool definitions from a prompt
 * when tool execution fails and a fallback to plain inference is needed.
 *
 * @see EnginePipelineContext
 * @see AbstractEngine
 * @since 1.0.0
 */
final class EnginePipelineBridge {

  /** Private constructor — utility class, not instantiable. */
  private EnginePipelineBridge() {}

  /**
   * Compiles a full inference context from the raw request through the pipeline.
   *
   * @param request The incoming internal chat request.
   * @param pipeline The orchestration pipeline (nullable — bypassed if null).
   * @param registry The model registry for provider/option resolution.
   * @param interceptors Ordered interceptors for pre-processing the prompt and options.
   * @return A fully compiled {@link EnginePipelineContext} ready for model invocation.
   */
  static EnginePipelineContext compileContext(
      InternalChatRequest request,
      OrchestrationPipeline pipeline,
      EngineModelRegistry registry,
      List<ContextInterceptor> interceptors) {

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
    ChatOptions springOptions = OptionsMapper.mapOptions(request.options(), provider);

    ChatOptions finalOptions =
        interceptors.stream()
            .reduce(
                springOptions,
                (opts, interceptor) -> interceptor.preProcess(request, promptText, messages, opts),
                (o1, o2) -> o1);

    Prompt prompt = new Prompt(messages, finalOptions);
    String conversationId =
        Optional.ofNullable(request.context())
            .map(com.orasaka.core.support.Context::conversationId)
            .orElse(null);

    return new EnginePipelineContext(provider, conversationId, promptText, prompt);
  }

  /**
   * Maps an Orasaka chat message to a Spring AI {@link Message} based on the role string.
   *
   * @param msg The Orasaka chat message containing role and content.
   * @return The corresponding Spring AI message ({@link SystemMessage}, {@link AssistantMessage},
   *     or {@link UserMessage}).
   */
  static Message mapMessage(InternalChatRequest.ChatMessage msg) {
    return switch (msg.role().toLowerCase()) {
      case "system" -> new SystemMessage(msg.content());
      case "assistant" -> new AssistantMessage(msg.content());
      default -> new UserMessage(msg.content());
    };
  }

  /**
   * Strips tool/function-calling definitions from a {@link Prompt}, preserving only the core model,
   * temperature, and token limit settings.
   *
   * <p>This is used as a fallback when tool execution activation fails with {@link
   * IllegalStateException}, allowing inference to proceed without tools.
   *
   * @param prompt The original prompt potentially containing tool definitions.
   * @return A new prompt with clean options (no tool definitions).
   */
  static Prompt removeTools(Prompt prompt) {
    ChatOptions options = prompt.getOptions();
    if (options == null) {
      return prompt;
    }
    ChatOptions cleanOptions =
        switch (options) {
          case org.springframework.ai.ollama.api.OllamaChatOptions ollama ->
              org.springframework.ai.ollama.api.OllamaChatOptions.builder()
                  .model(ollama.getModel())
                  .temperature(ollama.getTemperature())
                  .numPredict(ollama.getNumPredict())
                  .build();
          case org.springframework.ai.openai.OpenAiChatOptions openai ->
              org.springframework.ai.openai.OpenAiChatOptions.builder()
                  .model(openai.getModel())
                  .temperature(openai.getTemperature())
                  .maxTokens(openai.getMaxTokens())
                  .build();
          default -> options;
        };
    return new Prompt(prompt.getInstructions(), cleanOptions);
  }
}
