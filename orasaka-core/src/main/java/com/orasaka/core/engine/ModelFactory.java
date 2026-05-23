package com.orasaka.core.engine;

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
      String baseUrl = Optional.ofNullable(ollama.baseUrl()).orElse("http://localhost:11434");
      String model = Optional.ofNullable(ollama.model()).orElse("llama3:8b");
      OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
      OllamaChatOptions.Builder opts = OllamaChatOptions.builder().model(model);
      Optional.ofNullable(ollama.temperature()).ifPresent(opts::temperature);
      models.put(
          "ollama", OllamaChatModel.builder().ollamaApi(api).defaultOptions(opts.build()).build());
    }

    CoreProperties.ProviderConfig openai = properties.overrides().get("openai");
    if (openai != null && openai.baseUrl() != null) {
      String apiKey = Optional.ofNullable(System.getenv("OPENAI_API_KEY")).orElse("dummy-key");
      OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", OpenAiChatModel.builder().openAiApi(api).build());
    }

    return models;
  }

  static Map<String, EmbeddingModel> createEmbeddingModels(CoreProperties properties) {
    Map<String, EmbeddingModel> models = new HashMap<>();

    CoreProperties.ProviderConfig ollama = properties.overrides().get("ollama");
    if (ollama != null) {
      String baseUrl = Optional.ofNullable(ollama.baseUrl()).orElse("http://localhost:11434");
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
      String apiKey = Optional.ofNullable(System.getenv("OPENAI_API_KEY")).orElse("dummy-key");
      OpenAiImageApi api =
          OpenAiImageApi.builder().apiKey(apiKey).baseUrl(openai.baseUrl()).build();
      models.put("openai", new OpenAiImageModel(api));
    }

    return models;
  }
}
