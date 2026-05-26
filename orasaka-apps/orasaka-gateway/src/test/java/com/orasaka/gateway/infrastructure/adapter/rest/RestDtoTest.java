package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.identity.infrastructure.support.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestDtoTest {

  @Test
  @DisplayName("FeatureResponse validates null constraints in constructor")
  void testFeatureResponse() {
    FeatureResponse valid = new FeatureResponse("1", "Label", "Icon", "/path", "GET", "{}");
    assertEquals("1", valid.id());
    assertEquals("Label", valid.label());
    assertEquals("Icon", valid.icon());
    assertEquals("/path", valid.uriPath());
    assertEquals("GET", valid.httpMethod());
    assertEquals("{}", valid.payloadTemplate());

    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse(null, "Label", "Icon", "/path", "GET", "{}"));
    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse("1", null, "Icon", "/path", "GET", "{}"));
    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse("1", "Label", null, "/path", "GET", "{}"));
    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse("1", "Label", "Icon", null, "GET", "{}"));
    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse("1", "Label", "Icon", "/path", null, "{}"));
    assertThrows(
        NullPointerException.class,
        () -> new FeatureResponse("1", "Label", "Icon", "/path", "GET", null));
  }

  @Test
  @DisplayName("ImageGenerationRequest validates blank constraints in constructor")
  void testImageGenerationRequest() {
    ImageGenerationRequest valid = new ImageGenerationRequest("Generate poster", "dall-e-3");
    assertEquals("Generate poster", valid.prompt());
    assertEquals("dall-e-3", valid.model());

    assertThrows(InvalidRequestException.class, () -> new ImageGenerationRequest(null, "dall-e-3"));
    assertThrows(InvalidRequestException.class, () -> new ImageGenerationRequest(" ", "dall-e-3"));
  }

  @Test
  @DisplayName("TtsRequest validates blank constraints in constructor")
  void testTtsRequest() {
    TtsRequest valid = new TtsRequest("Hello world", "tts-1", "alloy");
    assertEquals("Hello world", valid.text());
    assertEquals("tts-1", valid.model());
    assertEquals("alloy", valid.voice());

    assertThrows(InvalidRequestException.class, () -> new TtsRequest(null, "tts-1", "alloy"));
    assertThrows(InvalidRequestException.class, () -> new TtsRequest("   ", "tts-1", "alloy"));
  }
}
