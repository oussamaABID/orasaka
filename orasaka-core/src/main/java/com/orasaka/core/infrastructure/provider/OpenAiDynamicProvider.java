package com.orasaka.core.infrastructure.provider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/** Dynamic ChatModel provider for OpenAI models. */
@Component
public class OpenAiDynamicProvider implements DynamicChatModelProvider {

  @Override
  public boolean supports(String providerName) {
    return "openai".equalsIgnoreCase(providerName);
  }

  @Override
  public ChatModel create(String modelName, String apiKey) {
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("Model name is required for OpenAI dynamic provider");
    }
    var api = OpenAiApi.builder().apiKey(apiKey).build();
    var options = OpenAiChatOptions.builder().model(modelName).build();
    return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options).build();
  }
}
