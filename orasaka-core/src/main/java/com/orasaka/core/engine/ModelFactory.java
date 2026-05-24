package com.orasaka.core.engine;

import com.orasaka.core.support.OrasakaException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;

final class ModelFactory {

  private ModelFactory() {}

  static Map<String, ChatModel> createChatModels(CoreProperties properties) {
    Map<String, ChatModel> models = new HashMap<>();

    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl =
          Optional.ofNullable(ollama.baseUrl())
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () ->
                      new OrasakaException(
                          "Missing required property: orasaka.core.overrides.ollama.base-url"));
      String model = Optional.ofNullable(ollama.model()).orElse("llama3:8b");
      OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaChatOptions.Builder opts = OllamaChatOptions.builder().model(model);
      Optional.ofNullable(ollama.temperature()).ifPresent(opts::temperature);
      models.put(
          "ollama", OllamaChatModel.builder().ollamaApi(api).defaultOptions(opts.build()).build());
    }

    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey =
          Optional.ofNullable(openai.apiKey())
              .filter(k -> !k.isBlank())
              .orElseThrow(
                  () ->
                      new OrasakaException(
                          "Critical Security Violation: OpenAI API Key is unresolved."
                              + " Check orasaka.core.overrides.openai.api-key configuration."));
      OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", OpenAiChatModel.builder().openAiApi(api).build());
    }

    return models;
  }

  static Map<String, EmbeddingModel> createEmbeddingModels(CoreProperties properties) {
    Map<String, EmbeddingModel> models = new HashMap<>();

    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl =
          Optional.ofNullable(ollama.baseUrl())
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () ->
                      new OrasakaException(
                          "Missing required property: orasaka.core.overrides.ollama.base-url"));
      String model =
          Optional.ofNullable(ollama.extra())
              .map(e -> e.get("embedding-model"))
              .map(Object::toString)
              .orElse("all-minilm");
      OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaEmbeddingOptions opts = OllamaEmbeddingOptions.builder().model(model).build();
      models.put(
          "ollama", OllamaEmbeddingModel.builder().ollamaApi(api).defaultOptions(opts).build());
    }

    return models;
  }

  static Map<String, ImageModel> createImageModels(CoreProperties properties) {
    Map<String, ImageModel> models = new HashMap<>();

    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey =
          Optional.ofNullable(openai.apiKey())
              .filter(k -> !k.isBlank())
              .orElseThrow(
                  () ->
                      new OrasakaException(
                          "Critical Security Violation: OpenAI API Key is unresolved."
                              + " Check orasaka.core.overrides.openai.api-key configuration."));
      OpenAiImageApi api =
          OpenAiImageApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", new OpenAiImageModel(api));
    }

    return models;
  }
}
