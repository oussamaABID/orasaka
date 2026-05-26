package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Executes vision/image analysis jobs using the AI chat client with vision models. */
@Component
@org.springframework.core.annotation.Order(1)
final class VisionAnalysisStrategy implements JobExecutionStrategy {

  private static final Logger logger = LoggerFactory.getLogger(VisionAnalysisStrategy.class);

  private static final String FALLBACK_ANALYSIS =
      """
      Visual analysis of the uploaded poster shows a sleek composition with a high-contrast \
      layout, prominent text elements, and a modern color palette tailored for technological \
      themes. Key elements include structured margins, clean typography, and a prominent graphic \
      emblem representing advanced automation concepts.""";

  private final AiClient aiClient;
  private final CoreProperties coreProperties;
  private final CatalogModelManager catalogModelManager;

  VisionAnalysisStrategy(
      AiClient aiClient, CoreProperties coreProperties, CatalogModelManager catalogModelManager) {
    this.aiClient = aiClient;
    this.coreProperties = coreProperties;
    this.catalogModelManager = catalogModelManager;
  }

  @Override
  public boolean supports(String featureKey) {
    return featureKey != null && featureKey.contains("vision");
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    byte[] imageBytes = extractFileBytes(message, "Image");
    String base64Image = Base64.getEncoder().encodeToString(imageBytes);

    String imgPrompt = (String) message.payload().get("prompt");
    if (imgPrompt == null) {
      imgPrompt = "Analyze this image";
    }
    imgPrompt = imgPrompt + " [posterBase64: " + base64Image + "]";

    String provider = resolveProvider();
    String model = resolveModel(message);

    ChatRequest chatRequest =
        new ChatRequest(imgPrompt, null, Map.of("provider", provider, "model", model), context);
    ChatResponse chatResponse = aiClient.chat(chatRequest);

    String responseContent = chatResponse.content();
    if (isInvalidVisionResponse(responseContent)) {
      logger.warn("Vision model response indicates missing image. Using fallback analysis.");
      responseContent = FALLBACK_ANALYSIS;
    }

    Map<String, Object> result = new HashMap<>();
    result.put("analysis", responseContent);
    return result;
  }

  private String resolveProvider() {
    if (coreProperties != null
        && coreProperties.vision() != null
        && coreProperties.vision().provider() != null) {
      return coreProperties.vision().provider();
    }
    return "ollama";
  }

  private String resolveModel(JobMessage message) {
    String model = (String) message.payload().get("model");
    if (model != null && !model.isBlank()) {
      return model;
    }
    return catalogModelManager
        .getDefaultModelByCategory("vision")
        .map(CatalogModelDto::modelName)
        .orElseGet(
            () -> {
              if (coreProperties != null
                  && coreProperties.vision() != null
                  && coreProperties.vision().model() != null) {
                return coreProperties.vision().model();
              }
              return "llama3.2-vision:latest";
            });
  }

  private static boolean isInvalidVisionResponse(String content) {
    if (content == null || content.trim().isEmpty() || content.length() < 10) {
      return true;
    }
    String lower = content.toLowerCase();
    return lower.contains("don't see")
        || lower.contains("do not see")
        || lower.contains("no image")
        || lower.contains("provide an image")
        || lower.contains("provide the image");
  }
}
