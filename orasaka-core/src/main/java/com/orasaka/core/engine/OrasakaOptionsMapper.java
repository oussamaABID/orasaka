package com.orasaka.core.engine;

import com.orasaka.core.model.DefaultOrasakaOptions;
import com.orasaka.core.model.OrasakaImageRequest;
import com.orasaka.core.model.OrasakaOptions;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageOptions;

/** Utility class responsible for mapping Orasaka options to Spring AI options. */
public final class OrasakaOptionsMapper {

  private OrasakaOptionsMapper() {
    // Utility
  }

  /** Maps OrasakaOptions to Spring AI ChatOptions. */
  public static ChatOptions mapOptions(OrasakaOptions options, String provider) {
    Double temp =
        switch (options) {
          case null -> 0.7;
          case DefaultOrasakaOptions def -> def.temperature() != null ? def.temperature() : 0.7;
          case OrasakaOptions o -> o.getTemperature() != null ? o.getTemperature() : 0.7;
        };

    Integer tokens =
        switch (options) {
          case null -> null;
          case DefaultOrasakaOptions def -> def.maxTokens();
          case OrasakaOptions o -> o.getMaxTokens();
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

  /** Maps OrasakaImageRequest parameters to provider-specific Spring AI image options. */
  public static ImageOptions mapImageOptions(OrasakaImageRequest request, String provider) {
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
