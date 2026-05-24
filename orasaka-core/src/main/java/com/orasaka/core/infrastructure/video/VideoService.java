package com.orasaka.core.infrastructure.video;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.ingest.video.VideoRequest;
import com.orasaka.core.ingest.video.VideoResponse;
import com.orasaka.core.support.CoreException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for AI-powered video generation via the LocalAI bare-metal REST API.
 *
 * <p>Uses a {@link RestClient} to call the {@code /v1/videos/generations} endpoint, sending a text
 * prompt and duration parameter, and decoding the base64-encoded MP4 response.
 *
 * <p>Video generation is gated by the {@code orasaka.core.video.generation.enabled} configuration
 * flag; calls made when disabled throw {@link NonTransientAiException}.
 *
 * @see VideoRequest
 * @see VideoResponse
 * @since 1.0.0
 */
@Service
public class VideoService {
  /** The configured REST client pointing to the LocalAI video endpoint. */
  private final RestClient restClient;

  /** Whether video generation is enabled via configuration. */
  private final boolean generationEnabled;

  /**
   * Constructs the video service with a RestClient builder and core properties.
   *
   * @param builder Spring RestClient builder for HTTP client construction.
   * @param properties Core configuration for provider URLs and feature flags.
   * @throws CoreException If the {@code localai-video} base URL is missing.
   */
  public VideoService(RestClient.Builder builder, CoreProperties properties) {
    this.generationEnabled = resolveGenerationEnabled(properties);
    String targetUrl = resolveBaseUrl(properties);
    this.restClient = builder.baseUrl(targetUrl).build();
  }

  /**
   * Generates a video from a text prompt using the LocalAI inference endpoint.
   *
   * @param request The video generation request with prompt and duration.
   * @return The generated video response containing raw MP4 bytes.
   * @throws NonTransientAiException If video generation is disabled or the inference call fails.
   */
  public VideoResponse generateVideo(VideoRequest request) {
    if (!generationEnabled) {
      throw new NonTransientAiException("Video generation is disabled via configuration");
    }

    Map<String, Object> payload =
        Map.of("prompt", request.prompt(), "video_length", request.durationSeconds());

    try {
      Map<String, Object> response = executeInference(payload);
      String base64Video = extractBase64Video(response);
      return new VideoResponse(Base64.getDecoder().decode(base64Video.trim()), "mp4");
    } catch (NonTransientAiException e) {
      throw e;
    } catch (Exception e) {
      throw new NonTransientAiException("Native bare-metal video inference call failed", e);
    }
  }

  /**
   * Executes the HTTP POST inference call to the LocalAI video endpoint.
   *
   * @param payload The JSON payload map with prompt and video_length.
   * @return The parsed response map containing the "data" array.
   * @throws IllegalStateException If the response body is empty or missing the data key.
   */
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

  /**
   * Extracts the base64-encoded video string from the inference response.
   *
   * @param response The parsed response map from the inference endpoint.
   * @return The base64-encoded video string.
   * @throws IllegalStateException If the data array is empty or missing the b64_json field.
   */
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

  /**
   * Resolves whether video generation is enabled from configuration properties.
   *
   * @param properties Core configuration properties.
   * @return {@code true} if video generation is explicitly enabled.
   */
  private static boolean resolveGenerationEnabled(CoreProperties properties) {
    return properties.video() != null
        && properties.video().generation() != null
        && properties.video().generation().enabled();
  }

  /**
   * Resolves the base URL for the LocalAI video endpoint.
   *
   * @param properties Core configuration properties.
   * @return The validated base URL string.
   * @throws CoreException If the {@code localai-video} base URL is missing.
   */
  private static String resolveBaseUrl(CoreProperties properties) {
    var videoConfig =
        properties.overrides() != null ? properties.overrides().get("localai-video") : null;
    if (videoConfig == null || videoConfig.baseUrl() == null || videoConfig.baseUrl().isBlank()) {
      throw new CoreException(
          "Missing required property: orasaka.core.overrides.localai-video.base-url");
    }
    return videoConfig.baseUrl();
  }
}
