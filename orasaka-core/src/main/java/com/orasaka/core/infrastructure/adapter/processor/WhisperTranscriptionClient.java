package com.orasaka.core.infrastructure.adapter.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Spring {@link RestClient}-based helper for Whisper audio transcription calls.
 *
 * <p>Encapsulates the multipart/form-data upload to a LocalAI-compatible {@code
 * /v1/audio/transcriptions} endpoint using Spring-native {@link ByteArrayResource} — eliminating
 * all manual boundary forging.
 *
 * <p>Consumed by {@link LocalAudioProcessor} and {@link LocalVideoProcessor} via Spring DI.
 */
public final class WhisperTranscriptionClient {

  private static final Logger logger = LoggerFactory.getLogger(WhisperTranscriptionClient.class);
  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 60_000;

  private final RestClient.Builder restClientBuilder;
  private final ObjectMapper objectMapper;

  public WhisperTranscriptionClient(
      RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
    this.restClientBuilder =
        Objects.requireNonNull(restClientBuilder, "RestClient.Builder must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
  }

  /**
   * Sends a multipart transcription request to the Whisper-compatible endpoint.
   *
   * @param baseUrl The base URL of the transcription service (e.g. {@code http://localhost:8085}).
   * @param audioBytes The raw audio binary content (extracted from media).
   * @param filename The virtual filename for the upload (e.g. {@code speech.mp3}).
   * @param model The Whisper model identifier (e.g. {@code whisper-1}).
   * @return The transcribed text extracted from the JSON response.
   * @throws IllegalStateException If the response is missing or unparseable.
   */
  String transcribe(String baseUrl, byte[] audioBytes, String filename, String model) {
    if (filename == null || !isAudioFile(filename)) {
      throw new IllegalArgumentException(
          "Whisper transcription only accepts audio files. Rejected: " + filename);
    }

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MS);

    RestClient client =
        restClientBuilder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();

    ByteArrayResource fileResource = new NamedByteArrayResource(audioBytes, filename);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", fileResource);
    body.add("model", model);

    logger.info(
        "WhisperTranscriptionClient: Transcribing {} bytes via {} at {}...",
        audioBytes.length,
        model,
        baseUrl);

    String responseBody =
        client
            .post()
            .uri("/v1/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String.class);

    if (responseBody == null || responseBody.isBlank()) {
      throw new IllegalStateException("Whisper transcription response is empty");
    }

    logger.info("Whisper transcription response: {}", responseBody);
    return parseTranscriptionText(responseBody);
  }

  private boolean isAudioFile(String filename) {
    String lower = filename.toLowerCase();
    return lower.endsWith(".mp3")
        || lower.endsWith(".wav")
        || lower.endsWith(".m4a")
        || lower.endsWith(".ogg")
        || lower.endsWith(".flac")
        || lower.endsWith(".aac")
        || lower.endsWith(".webm");
  }

  private String parseTranscriptionText(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      if (root.has("text")) {
        return root.get("text").asText();
      }
      return responseBody;
    } catch (Exception e) {
      logger.warn("Failed to parse Whisper JSON response, returning raw body", e);
      return responseBody;
    }
  }

  /**
   * Named {@link ByteArrayResource} subclass that provides a filename for multipart uploads.
   * Replaces anonymous subclass to comply with ArchUnit governance mandate.
   */
  static final class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    NamedByteArrayResource(byte[] byteArray, String filename) {
      super(byteArray);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof NamedByteArrayResource other)) return false;
      if (!super.equals(other)) return false;
      return Objects.equals(filename, other.filename);
    }

    @Override
    public int hashCode() {
      return 31 * super.hashCode() + Objects.hashCode(filename);
    }
  }
}
