package com.orasaka.core.engine.video;

import com.orasaka.core.engine.CoreProperties;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrasakaVideoService {
  private final RestClient restClient;

  public OrasakaVideoService(RestClient.Builder builder, CoreProperties properties) {
    var videoConfig =
        properties.overrides() != null ? properties.overrides().get("localai-video") : null;
    String targetUrl =
        videoConfig != null && videoConfig.baseUrl() != null
            ? videoConfig.baseUrl()
            : "http://localhost:8086";
    this.restClient = builder.baseUrl(targetUrl).build();
  }

  @SuppressWarnings("unchecked")
  public OrasakaVideoResponse generateVideo(OrasakaVideoRequest request) {
    Map<String, Object> payload =
        Map.of(
            "prompt",
            request.prompt(),
            "video_length",
            request.durationSeconds() != null ? request.durationSeconds() : 4);

    try {
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

      List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
      if (dataList == null || dataList.isEmpty()) {
        throw new IllegalStateException("Response data array is empty");
      }

      String base64Video = (String) dataList.get(0).get("b64_json");
      if (base64Video == null || base64Video.isBlank()) {
        throw new IllegalStateException("Response base64 video string is missing or empty");
      }

      return new OrasakaVideoResponse(Base64.getDecoder().decode(base64Video.trim()), "mp4");
    } catch (Exception e) {
      throw new NonTransientAiException("Native bare-metal video inference call failed", e);
    }
  }
}
