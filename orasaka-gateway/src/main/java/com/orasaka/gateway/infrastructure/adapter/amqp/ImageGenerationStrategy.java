package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executes image generation jobs via the AI client's image endpoint.
 *
 */
@Component
@org.springframework.core.annotation.Order(1)
final class ImageGenerationStrategy implements JobExecutionStrategy {

  private final AiClient aiClient;
  private final CatalogModelManager catalogModelManager;
  private final String uploadDir;

  ImageGenerationStrategy(
      AiClient aiClient,
      CatalogModelManager catalogModelManager,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDir) {
    this.aiClient = aiClient;
    this.catalogModelManager = catalogModelManager;
    this.uploadDir = uploadDir;
  }

  @Override
  public boolean supports(String featureKey) {
    return featureKey != null && featureKey.contains("image");
  }

  @Override
  public Map<String, Object> execute(JobMessage message, Context context)
      throws JobExecutionException {
    String prompt = JobStrategyHelper.extractPrompt(message);

    String model =
        JobStrategyHelper.resolveModel(
            message, "image", "stable-diffusion-xl", catalogModelManager);
    ImageRequest imageRequest = new ImageRequest(prompt, null, null, model, Map.of(), context);
    ImageResponse imageResponse = aiClient.image(imageRequest);

    String translatedUrl = "";
    byte[] imgData = imageResponse.imageData();

    if (imgData == null && imageResponse.url() != null) {
      String url = imageResponse.url().trim();
      if (url.startsWith("data:")) {
        imgData = decodeDataUrl(url);
      } else if (url.startsWith("http://") || url.startsWith("https://")) {
        translatedUrl = url;
      }
    }

    if (imgData != null && imgData.length > 0) {
      translatedUrl =
          JobMediaHelper.saveMediaToFile(
              uploadDir, message.userId(), message.jobId(), imgData, "image.png");
    } else if (translatedUrl.isEmpty() && imageResponse.url() != null) {
      translatedUrl = imageResponse.url();
    }

    Map<String, Object> result = new HashMap<>();
    result.put("url", translatedUrl);
    result.put("format", imageResponse.format() != null ? imageResponse.format() : "png");
    return result;
  }

  private static byte[] decodeDataUrl(String url) {
    try {
      String base64 = url.substring(url.indexOf(",") + 1);
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      LoggerFactory.getLogger(ImageGenerationStrategy.class)
          .warn("Failed to decode inline data URL", e);
      return new byte[0];
    }
  }
}
