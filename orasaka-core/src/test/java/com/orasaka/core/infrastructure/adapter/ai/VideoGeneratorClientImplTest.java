package com.orasaka.core.infrastructure.adapter.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.support.CoreException;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class VideoGeneratorClientImplTest {

  private RestClient.Builder restClientBuilder;
  private RestClient restClient;
  private CoreProperties coreProperties;
  private CoreProperties.VideoConfig videoConfig;
  private CoreProperties.VideoGenerationConfig videoGenerationConfig;
  private ObjectMapper objectMapper;
  private CatalogModelManager catalogModelManager;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    restClientBuilder = mock(RestClient.Builder.class);
    restClient = mock(RestClient.class);
    coreProperties = mock(CoreProperties.class);
    videoConfig = mock(CoreProperties.VideoConfig.class);
    videoGenerationConfig = mock(CoreProperties.VideoGenerationConfig.class);
    objectMapper = new ObjectMapper();
    catalogModelManager = mock(CatalogModelManager.class);

    when(restClientBuilder.clone()).thenReturn(restClientBuilder);
    when(restClientBuilder.baseUrl(any(String.class))).thenReturn(restClientBuilder);
    when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    when(coreProperties.video()).thenReturn(videoConfig);
    when(videoConfig.generation()).thenReturn(videoGenerationConfig);
    when(videoGenerationConfig.baseUrl()).thenReturn("http://localhost:8188");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGenerateVideoSuccessfullyFromPrimary() {
    // Arrange
    VideoRequest request = new VideoRequest("Cyberpunk city", 5, Map.of(), Context.anonymous());
    VideoGeneratorClientImpl client =
        new VideoGeneratorClientImpl(
            restClientBuilder,
            coreProperties,
            objectMapper,
            catalogModelManager,
            "http://localhost:8085");

    RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Map> responseEntity = mock(ResponseEntity.class);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri("/v1/videos/generations")).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(String.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(any(Class.class))).thenReturn((ResponseEntity) responseEntity);

    String dummyBase64 = Base64.getEncoder().encodeToString("fake-video-bytes".getBytes());
    Map<String, Object> responseBody = Map.of("data", List.of(Map.of("b64_json", dummyBase64)));
    when(responseEntity.getBody()).thenReturn(responseBody);

    // Act
    VideoResponse response = client.generateVideo(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(new String(response.videoData())).isEqualTo("fake-video-bytes");
    assertThat(response.format()).isEqualTo("mp4");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallbackToSdServerWhenPrimaryFails() {
    // Arrange
    VideoRequest request = new VideoRequest("Cyberpunk city", 5, Map.of(), Context.anonymous());

    RestClient primaryRestClient = mock(RestClient.class);
    RestClient fallbackRestClient = mock(RestClient.class);
    when(restClientBuilder.build()).thenReturn(primaryRestClient, fallbackRestClient);

    VideoGeneratorClientImpl client =
        new VideoGeneratorClientImpl(
            restClientBuilder,
            coreProperties,
            objectMapper,
            catalogModelManager,
            "http://localhost:8085");

    // Mock primary to fail (return empty response)
    RestClient.RequestBodyUriSpec primaryUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec primaryBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec primaryResponseSpec = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Map> primaryResponseEntity = mock(ResponseEntity.class);

    when(primaryRestClient.post()).thenReturn(primaryUriSpec);
    when(primaryUriSpec.uri("/v1/videos/generations")).thenReturn(primaryBodySpec);
    when(primaryBodySpec.contentType(any())).thenReturn(primaryBodySpec);
    when(primaryBodySpec.body(any(String.class))).thenReturn(primaryBodySpec);
    when(primaryBodySpec.retrieve()).thenReturn(primaryResponseSpec);
    when(primaryResponseSpec.toEntity(any(Class.class)))
        .thenReturn((ResponseEntity) primaryResponseEntity);
    when(primaryResponseEntity.getBody()).thenReturn(null);

    // Mock fallback to succeed
    RestClient.RequestBodyUriSpec fallbackUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec fallbackBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec fallbackResponseSpec = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Map> fallbackResponseEntity = mock(ResponseEntity.class);

    when(fallbackRestClient.post()).thenReturn(fallbackUriSpec);
    when(fallbackUriSpec.uri("/v1/images/generations")).thenReturn(fallbackBodySpec);
    when(fallbackBodySpec.body(any(Map.class))).thenReturn(fallbackBodySpec);
    when(fallbackBodySpec.retrieve()).thenReturn(fallbackResponseSpec);
    when(fallbackResponseSpec.toEntity(any(Class.class)))
        .thenReturn((ResponseEntity) fallbackResponseEntity);

    String dummyBase64 = Base64.getEncoder().encodeToString("fallback-video-bytes".getBytes());
    Map<String, Object> fallbackResponseBody =
        Map.of("data", List.of(Map.of("b64_json", dummyBase64)));
    when(fallbackResponseEntity.getBody()).thenReturn(fallbackResponseBody);

    // Act
    VideoResponse response = client.generateVideo(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(new String(response.videoData())).isEqualTo("fallback-video-bytes");
    assertThat(response.format()).isEqualTo("mp4");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFailWhenPrimaryResponseIsEmptyAndFallbackFails() {
    // Arrange
    VideoRequest request = new VideoRequest("Cyberpunk city", 5, Map.of(), Context.anonymous());

    RestClient primaryRestClient = mock(RestClient.class);
    RestClient fallbackRestClient = mock(RestClient.class);
    when(restClientBuilder.build()).thenReturn(primaryRestClient, fallbackRestClient);

    VideoGeneratorClientImpl client =
        new VideoGeneratorClientImpl(
            restClientBuilder,
            coreProperties,
            objectMapper,
            catalogModelManager,
            "http://localhost:8085");

    // Mock primary to fail (return empty response)
    RestClient.RequestBodyUriSpec primaryUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec primaryBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec primaryResponseSpec = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Map> primaryResponseEntity = mock(ResponseEntity.class);

    when(primaryRestClient.post()).thenReturn(primaryUriSpec);
    when(primaryUriSpec.uri("/v1/videos/generations")).thenReturn(primaryBodySpec);
    when(primaryBodySpec.contentType(any())).thenReturn(primaryBodySpec);
    when(primaryBodySpec.body(any(String.class))).thenReturn(primaryBodySpec);
    when(primaryBodySpec.retrieve()).thenReturn(primaryResponseSpec);
    when(primaryResponseSpec.toEntity(any(Class.class)))
        .thenReturn((ResponseEntity) primaryResponseEntity);
    when(primaryResponseEntity.getBody()).thenReturn(null);

    // Mock fallback to fail (return empty response)
    RestClient.RequestBodyUriSpec fallbackUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec fallbackBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec fallbackResponseSpec = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Map> fallbackResponseEntity = mock(ResponseEntity.class);

    when(fallbackRestClient.post()).thenReturn(fallbackUriSpec);
    when(fallbackUriSpec.uri("/v1/images/generations")).thenReturn(fallbackBodySpec);
    when(fallbackBodySpec.body(any(Map.class))).thenReturn(fallbackBodySpec);
    when(fallbackBodySpec.retrieve()).thenReturn(fallbackResponseSpec);
    when(fallbackResponseSpec.toEntity(any(Class.class)))
        .thenReturn((ResponseEntity) fallbackResponseEntity);
    when(fallbackResponseEntity.getBody()).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> client.generateVideo(request))
        .isInstanceOf(CoreException.class)
        .hasMessageContaining("Both primary video inference and fallback sd-server calls failed");
  }
}
