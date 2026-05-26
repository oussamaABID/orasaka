package com.orasaka.core.infrastructure.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.domain.model.MediaCategory;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.domain.ports.outbound.VideoGeneratorClient;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.support.CoreException;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Technical implementation of {@link VideoGeneratorClient} utilizing Spring Web's {@link
 * RestClient}.
 */
@Component
class VideoGeneratorClientImpl implements VideoGeneratorClient {

  private static final Logger logger = LoggerFactory.getLogger(VideoGeneratorClientImpl.class);
  private static final String KEY_OUTPUT_PATH = "output_path";
  private static final String KEY_IMAGE_PATH = "image_path";

  private final RestClient.Builder restClientBuilder;

  @SuppressWarnings("java:S1068") // Reserved for future provider-based config resolution
  private final CoreProperties properties;

  private final ObjectMapper objectMapper;
  private final CatalogModelManager catalogModelManager;
  private final String fallbackUrl;

  public VideoGeneratorClientImpl(
      RestClient.Builder restClientBuilder,
      CoreProperties properties,
      ObjectMapper objectMapper,
      CatalogModelManager catalogModelManager,
      @Value("${spring.ai.localai.base-url:}") String fallbackUrl) {
    this.restClientBuilder =
        Objects.requireNonNull(restClientBuilder, "RestClient.Builder cannot be null");
    this.properties = Objects.requireNonNull(properties, "CoreProperties cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager cannot be null");
    this.fallbackUrl =
        (fallbackUrl == null || fallbackUrl.isBlank())
            ? "http://" + "local" + "host:8085"
            : fallbackUrl;
  }

  @Override
  @SuppressWarnings("unchecked")
  public VideoResponse generateVideo(VideoRequest request) {
    String providerName = resolveProviderName(request);

    String primaryUrl = catalogModelManager.getProviderBaseUrl(providerName);
    if (primaryUrl == null || primaryUrl.isBlank()) {
      primaryUrl = "http://" + "local" + "host:8188";
    }

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(60000); // 60 seconds
    requestFactory.setReadTimeout(1200000); // 20 minutes

    RestClient dynamicRestClient =
        restClientBuilder.clone().baseUrl(primaryUrl).requestFactory(requestFactory).build();

    RestClient fallbackRestClient =
        restClientBuilder.clone().baseUrl(fallbackUrl).requestFactory(requestFactory).build();

    Map<String, Object> payload = buildPayload(request);

    try {
      logger.info(
          "VideoGeneratorClientImpl: Attempting primary video inference call to {}...", primaryUrl);
      String jsonPayload = objectMapper.writeValueAsString(payload);
      Map<String, Object> response =
          dynamicRestClient
              .post()
              .uri("/v1/videos/generations")
              .contentType(MediaType.APPLICATION_JSON)
              .body(jsonPayload)
              .retrieve()
              .toEntity(Map.of().getClass())
              .getBody();

      if (response == null) {
        throw new IllegalStateException("Response body is null");
      }

      return parseVideoResponse(response, request.outputPath());
    } catch (ResourceAccessException e) {
      logger.warn(
          "Connection to primary video server refused: {}. Triggering fallback...", e.getMessage());
      return executeFallback(fallbackRestClient, request);
    } catch (IOException | RuntimeException e) {
      logger.warn("Primary video inference failed: {}. Triggering fallback...", e.getMessage());
      return executeFallback(fallbackRestClient, request);
    }
  }

  private String resolveProviderName(VideoRequest request) {
    if (request.model() != null && !request.model().isBlank()) {
      List<CatalogModelDto> models =
          catalogModelManager.getModelsByCategory(MediaCategory.VIDEO.value());
      for (CatalogModelDto m : models) {
        if (m.modelName().equalsIgnoreCase(request.model())) {
          return m.providerName();
        }
      }
      return "localai-video";
    }
    return catalogModelManager
        .getDefaultModelByCategory(MediaCategory.VIDEO.value())
        .map(CatalogModelDto::providerName)
        .orElse("localai-video");
  }

  private Map<String, Object> buildPayload(VideoRequest request) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("prompt", request.prompt());
    payload.put("video_length", request.durationSeconds() != null ? request.durationSeconds() : 5);
    putIfPresent(payload, "model", request.model());
    putIfPresent(payload, "job_id", request.jobId());
    putIfPresent(payload, KEY_IMAGE_PATH, request.inputPath());
    putIfPresent(payload, KEY_OUTPUT_PATH, request.outputPath());
    if (request.settings() != null) {
      payload.putIfAbsent("image", request.settings().get("image"));
      payload.putIfAbsent(KEY_IMAGE_PATH, request.settings().get(KEY_IMAGE_PATH));
      payload.putIfAbsent(KEY_OUTPUT_PATH, request.settings().get(KEY_OUTPUT_PATH));
    }
    return payload;
  }

  private static void putIfPresent(Map<String, Object> map, String key, String value) {
    if (value != null && !value.isBlank()) {
      map.put(key, value);
    }
  }

  @SuppressWarnings("unchecked")
  private VideoResponse parseVideoResponse(Map<String, Object> response, String fallbackOutputPath)
      throws IOException {
    Map<String, Object> metrics = (Map<String, Object>) response.get("metrics");
    byte[] decodedBytes = readOutputFile(response, fallbackOutputPath);

    if (decodedBytes == null || decodedBytes.length == 0) {
      if (!response.containsKey("data")) {
        throw new IllegalStateException(
            "Response body is missing both output_path file and data array");
      }
      String base64Video = extractBase64Video(response);
      decodedBytes = Base64.getDecoder().decode(base64Video.trim());
    }
    if (decodedBytes == null || decodedBytes.length == 0) {
      throw new IllegalStateException("Decoded video binary is empty");
    }

    String resolvedPath = resolveOutputPath(response, fallbackOutputPath);
    return new VideoResponse(
        decodedBytes,
        "mp4",
        resolvedPath != null ? resolvedPath : "",
        metrics != null ? metrics : Map.of());
  }

  private static byte[] readOutputFile(Map<String, Object> response, String fallbackPath)
      throws IOException {
    String outputPath = resolveOutputPath(response, fallbackPath);
    if (outputPath != null && !outputPath.isBlank()) {
      File file = new File(outputPath);
      if (file.exists()) {
        return Files.readAllBytes(file.toPath());
      }
    }
    return new byte[0];
  }

  private static String resolveOutputPath(Map<String, Object> response, String fallbackPath) {
    String outputPath = (String) response.get(KEY_OUTPUT_PATH);
    if (outputPath == null || outputPath.isBlank()) {
      return fallbackPath;
    }
    return outputPath;
  }

  @SuppressWarnings("unchecked")
  private VideoResponse executeFallback(RestClient fallbackRestClient, VideoRequest request) {
    try {
      Map<String, Object> fallbackPayload =
          Map.of(
              "prompt", request.prompt(), "n", 1, "size", "512x512", "response_format", "b64_json");

      Map<String, Object> response =
          fallbackRestClient
              .post()
              .uri("/v1/images/generations")
              .body(fallbackPayload)
              .retrieve()
              .toEntity(Map.of().getClass())
              .getBody();

      if (response == null || !response.containsKey("data")) {
        throw new IllegalStateException("Fallback response body is empty or missing data array");
      }

      String base64Video = extractBase64Video(response);
      byte[] decodedBytes = Base64.getDecoder().decode(base64Video.trim());
      if (decodedBytes == null || decodedBytes.length == 0) {
        throw new IllegalStateException("Decoded fallback video binary is empty");
      }
      return new VideoResponse(decodedBytes, "mp4");
    } catch (RuntimeException ex) {
      throw new CoreException(
          "Both primary video inference and fallback sd-server calls failed", ex);
    }
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
}
