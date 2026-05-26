package com.orasaka.core.domain.model.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

class CatalogModelInfoTest {

  @Test
  @DisplayName("CatalogModelInfo constructor validates input fields and applies defaults")
  void validation() {
    CatalogModelInfo valid =
        new CatalogModelInfo(1, "gpt-4", "GPT-4 Model", CHAT, "{}", true, "openai");
    assertNotNull(valid);
    assertEquals("gpt-4", valid.modelName());
    assertEquals("openai", valid.providerName());
    assertTrue(valid.isDefault());

    // Validation failures
    assertThrows(
        NullPointerException.class,
        () -> new CatalogModelInfo(1, null, "label", "cat", "{}", true, "provider"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelInfo(1, "", "label", "cat", "{}", true, "provider"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelInfo(1, "   ", "label", "cat", "{}", true, "provider"));

    assertThrows(
        NullPointerException.class,
        () -> new CatalogModelInfo(1, "name", null, "cat", "{}", true, "provider"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelInfo(1, "name", "", "cat", "{}", true, "provider"));

    assertThrows(
        NullPointerException.class,
        () -> new CatalogModelInfo(1, "name", "label", null, "{}", true, "provider"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CatalogModelInfo(1, "name", "label", "", "{}", true, "provider"));

    // Defaults testing
    CatalogModelInfo defaults =
        new CatalogModelInfo(2, "llama-3", "Llama 3", CHAT, "{}", null, null);
    assertFalse(defaults.isDefault());
    assertEquals("ollama", defaults.providerName());

    // Backwards compatible constructor
    CatalogModelInfo compat = new CatalogModelInfo(3, "llama-3", "Llama 3", CHAT, "{}", null);
    assertFalse(compat.isDefault());
    assertEquals("ollama", compat.providerName());
  }
}
