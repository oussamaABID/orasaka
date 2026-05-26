package com.orasaka.core.infrastructure.adapter.ai;

import com.orasaka.core.domain.model.MediaCategory;
import com.orasaka.core.domain.model.image.ImageRequest;
import com.orasaka.core.domain.model.image.ImageResponse;
import com.orasaka.core.domain.ports.outbound.ImageGeneratorClient;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.config.SafeImageModel;
import com.orasaka.core.infrastructure.support.CoreException;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter implementation of {@link ImageGeneratorClient} wrapping Spring AI's {@link ImageModel}.
 */
@Component
class ImageGeneratorClientImpl implements ImageGeneratorClient {

  private static final Logger logger = LoggerFactory.getLogger(ImageGeneratorClientImpl.class);
  private static final int DEFAULT_TIMEOUT_MS = 180_000;

  private final CoreProperties properties;
  private final CatalogModelManager catalogModelManager;

  /**
   * Constructs the adapter with the CatalogModelManager and CoreProperties.
   *
   * @param properties Core configuration properties.
   * @param catalogModelManager The catalog model manager.
   */
  public ImageGeneratorClientImpl(
      CoreProperties properties, CatalogModelManager catalogModelManager) {
    this.properties = Objects.requireNonNull(properties, "CoreProperties must not be null");
    this.catalogModelManager =
        Objects.requireNonNull(catalogModelManager, "CatalogModelManager must not be null");
  }

  @Override
  public ImageResponse generateImage(ImageRequest request) {
    int height = request.height() != null ? request.height() : 512;
    int width = request.width() != null ? request.width() : 512;

    String activeModel = resolveModel(request.model());
    String providerName = resolveProviderName(activeModel);
    String baseUrl = resolveBaseUrl(providerName);
    String apiKey = resolveApiKey();

    logger.info(
        "ImageGeneratorClientImpl: Dynamically resolving provider baseUrl: {} for model: {}",
        baseUrl,
        activeModel);

    ImageModel dynamicImageModel = buildImageModel(baseUrl, apiKey, activeModel, height, width);

    OpenAiImageOptions executionOptions =
        OpenAiImageOptions.builder().model(activeModel).N(1).height(height).width(width).build();

    var response = dynamicImageModel.call(new ImagePrompt(request.prompt(), executionOptions));
    var generation = response.getResult();
    byte[] imageData = extractImageData(generation);
    String url = extractUrl(generation);

    if (imageData.length > 0 && url == null) {
      url = buildImageDataUrl(imageData);
    }
    return new ImageResponse(imageData, url, "png");
  }

  private String resolveModel(String requestModel) {
    if (requestModel != null && !requestModel.isBlank()) {
      return requestModel;
    }
    return catalogModelManager
        .getDefaultModelByCategory(MediaCategory.IMAGE.value())
        .map(CatalogModelDto::modelName)
        .orElse("stable-diffusion");
  }

  private String resolveProviderName(String activeModel) {
    List<CatalogModelDto> models =
        catalogModelManager.getModelsByCategory(MediaCategory.IMAGE.value());
    for (CatalogModelDto m : models) {
      if (m.modelName().equalsIgnoreCase(activeModel)) {
        return m.providerName();
      }
    }
    return "localai-image";
  }

  private String resolveBaseUrl(String providerName) {
    String baseUrl = catalogModelManager.getProviderBaseUrl(providerName);
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = resolveBaseUrlFromProperties();
    }
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new CoreException(
          "Missing required configuration: image generation baseUrl. "
              + "Configure via orasaka.core.image.generation.base-url or register a provider in the catalog.");
    }
    return baseUrl;
  }

  private String resolveBaseUrlFromProperties() {
    if (properties.image() != null
        && properties.image().generation() != null
        && properties.image().generation().baseUrl() != null) {
      return properties.image().generation().baseUrl();
    }
    return null;
  }

  private String resolveApiKey() {
    if (properties.image() != null
        && properties.image().generation() != null
        && properties.image().generation().apiKey() != null) {
      return properties.image().generation().apiKey();
    }
    return "not-required";
  }

  private ImageModel buildImageModel(
      String baseUrl, String apiKey, String activeModel, int height, int width) {
    int connectTimeout = resolveTimeout(true);
    int readTimeout = resolveTimeout(false);

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);

    RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

    OpenAiImageApi api =
        OpenAiImageApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .restClientBuilder(restClientBuilder)
            .build();

    OpenAiImageOptions executionOptions =
        OpenAiImageOptions.builder().model(activeModel).N(1).height(height).width(width).build();

    return new SafeImageModel(new OpenAiImageModel(api, executionOptions, new RetryTemplate()));
  }

  private int resolveTimeout(boolean isConnect) {
    if (properties.image() != null && properties.image().generation() != null) {
      var gen = properties.image().generation();
      Integer timeout = isConnect ? gen.connectTimeoutMs() : gen.readTimeoutMs();
      if (timeout != null) {
        return timeout;
      }
    }
    return DEFAULT_TIMEOUT_MS;
  }

  private byte[] extractImageData(ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) {
      return new byte[0];
    }
    byte[] data = decodeBase64Image(generation.getOutput().getB64Json());
    if (data.length == 0) {
      data = extractImageViaReflection(generation.getOutput());
    }
    return data;
  }

  private String extractUrl(ImageGeneration generation) {
    if (generation == null || generation.getOutput() == null) {
      return null;
    }
    return generation.getOutput().getUrl();
  }

  private byte[] decodeBase64Image(String b64) {
    if (b64 == null || b64.isBlank()) {
      return new byte[0];
    }
    try {
      return Base64.getDecoder().decode(b64.trim());
    } catch (IllegalArgumentException e) {
      logger.warn("Failed to decode base64 image data", e);
      return new byte[0];
    }
  }

  private byte[] extractImageViaReflection(Object output) {
    try {
      var method = output.getClass().getMethod("getImage");
      Object imgObj = method.invoke(output);
      if (imgObj instanceof byte[] bytes) {
        return bytes;
      }
    } catch (ReflectiveOperationException e) {
      logger.trace("Failed reflective fallback extraction via getImage()", e);
    }
    return new byte[0];
  }

  private String buildImageDataUrl(byte[] imageData) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
  }
}
