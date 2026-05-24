package com.orasaka.core.engine;

import com.orasaka.core.support.DefaultOptions;
import com.orasaka.core.support.InternalImageRequest;
import com.orasaka.core.support.Options;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;

/** Utility class responsible for mapping Orasaka options to Spring AI options. */
public final class OptionsMapper {

  private OptionsMapper() {}

  /**
   * Maps Orasaka-native chat options to Spring AI provider-specific {@link ChatOptions}.
   *
   * <p>Dispatches to the correct builder based on the provider name:
   *
   * <ul>
   *   <li>{@code "ollama"} → {@link OllamaChatOptions}
   *   <li>{@code "openai"} → {@link OpenAiChatOptions}
   *   <li>Other → {@link DefaultChatOptions}
   * </ul>
   *
   * @param options The Orasaka options (nullable — defaults to temperature 0.7).
   * @param provider The target provider name.
   * @return A provider-specific {@link ChatOptions} instance.
   */
  public static ChatOptions mapOptions(Options options, String provider) {
    Double temp =
        switch (options) {
          case null -> 0.7;
          case DefaultOptions def -> def.temperature() != null ? def.temperature() : 0.7;
          case Options o -> o.getTemperature() != null ? o.getTemperature() : 0.7;
        };

    Integer tokens =
        switch (options) {
          case null -> null;
          case DefaultOptions def -> def.maxTokens();
          case Options o -> o.getMaxTokens();
        };

    return switch (provider.toLowerCase()) {
      case "ollama" -> OllamaChatOptions.builder().temperature(temp).numPredict(tokens).build();
      case "openai" -> OpenAiChatOptions.builder().temperature(temp).maxTokens(tokens).build();
      default -> {
        DefaultChatOptions defaultOptions = new DefaultChatOptions();
        defaultOptions.setTemperature(temp);
        defaultOptions.setMaxTokens(tokens);
        yield defaultOptions;
      }
    };
  }

  /**
   * Maps image request parameters to provider-specific {@link ImageOptions}.
   *
   * <p>Currently only supports the {@code "openai"} provider with height, width, and quality
   * settings.
   *
   * @param request The internal image request containing dimension parameters.
   * @param provider The target provider name.
   * @return Provider-specific image options, or {@code null} for unsupported providers.
   */
  public static ImageOptions mapImageOptions(InternalImageRequest request, String provider) {
    return switch (provider.toLowerCase()) {
      case "openai" ->
          OpenAiImageOptions.builder()
              .height(request.height())
              .width(request.width())
              .quality("hd")
              .build();
      default -> null;
    };
  }
}
