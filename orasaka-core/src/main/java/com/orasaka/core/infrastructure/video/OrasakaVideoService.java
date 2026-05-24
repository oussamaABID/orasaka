package com.orasaka.core.infrastructure.video;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.ingest.video.OrasakaVideoRequest;
import com.orasaka.core.ingest.video.OrasakaVideoResponse;
import com.orasaka.core.support.OrasakaException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrasakaVideoService {
  private final RestClient restClient;
  private final boolean generationEnabled;

  public OrasakaVideoService(RestClient.Builder builder, CoreProperties properties) {
    this.generationEnabled = resolveGenerationEnabled(properties);
    String targetUrl = resolveBaseUrl(properties);
    this.restClient = builder.baseUrl(targetUrl).build();
  }

  @SuppressWarnings("unchecked")
  public OrasakaVideoResponse generateVideo(OrasakaVideoRequest request) {
    if (!generationEnabled) {
      throw new NonTransientAiException("Video generation is disabled via configuration");
    }

    Map<String, Object> payload =
        Map.of("prompt", request.prompt(), "video_length", request.durationSeconds());

    try {
      Map<String, Object> response = executeInference(payload);
      String base64Video = extractBase64Video(response);
      return new OrasakaVideoResponse(Base64.getDecoder().decode(base64Video.trim()), "mp4");
    } catch (NonTransientAiException e) {
      throw e;
    } catch (Exception e) {
      throw new NonTransientAiException("Native bare-metal video inference call failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> executeInference(Map<String, Object> payload) {
    Map<String, Object> response =
        restClient
            .post()
            .uri("/v1/videos/generations")
            .body(payload)
            .retrieve()
            .toEntity(Map.of().getClass())
            .getBody();
    if (response == null || !response.containsKey("data")) {
      throw new IllegalStateException("Response body is empty or missing data array");
    }
    return response;
  }

  @SuppressWarnings("unchecked")
  private static String extractBase64Video(Map<String, Object> response) {
    List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
    if (dataList == null || dataList.isEmpty()) {
      throw new IllegalStateException("Response data array is empty");
    }
    String base64Video = (String) dataList.get(0).get("b64_json");
    if (base64Video == null || base64Video.isBlank()) {
      throw new IllegalStateException("Response base64 video string is missing or empty");
    }
    return base64Video;
  }

  private static boolean resolveGenerationEnabled(CoreProperties properties) {
    return properties.video() != null
        && properties.video().generation() != null
        && properties.video().generation().enabled();
  }

  private static String resolveBaseUrl(CoreProperties properties) {
    var videoConfig =
        properties.overrides() != null ? properties.overrides().get("localai-video") : null;
    if (videoConfig == null || videoConfig.baseUrl() == null || videoConfig.baseUrl().isBlank()) {
      throw new OrasakaException(
          "Missing required property: orasaka.core.overrides.localai-video.base-url");
    }
    return videoConfig.baseUrl();
  }
}
