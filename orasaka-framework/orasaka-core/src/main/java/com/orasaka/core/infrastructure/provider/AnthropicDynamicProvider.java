package com.orasaka.core.infrastructure.provider;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Dynamic ChatModel provider for Anthropic (Claude) models. */
@Component
public class AnthropicDynamicProvider implements DynamicChatModelProvider {

  @Override
  public boolean supports(String providerName) {
    return "claude".equalsIgnoreCase(providerName) || "anthropic".equalsIgnoreCase(providerName);
  }

  @Override
  public ChatModel create(String modelName, String apiKey) {
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("Model name is required for Anthropic dynamic provider");
    }
    var api = AnthropicApi.builder().apiKey(apiKey).build();
    var options = AnthropicChatOptions.builder().model(modelName).build();
    return AnthropicChatModel.builder().anthropicApi(api).defaultOptions(options).build();
  }
}
