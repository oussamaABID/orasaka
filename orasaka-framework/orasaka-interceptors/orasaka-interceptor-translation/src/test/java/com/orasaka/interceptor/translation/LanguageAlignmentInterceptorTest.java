package com.orasaka.interceptor.translation;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.RoutingMode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LanguageAlignmentInterceptorTest {

  private final LanguageAlignmentInterceptor interceptor = new LanguageAlignmentInterceptor();

  @Test
  void getOrder_returns3() {
    assertEquals(3, interceptor.getOrder());
  }

  @Test
  void intercept_userLanguageIsEnglish_returnsContextUnchanged() {
    var context = new PromptContext("hello", Map.of("locale", "en"));
    PromptContext result = interceptor.intercept(context);
    assertSame(context, result);
  }

  @Test
  void intercept_noLanguageMetadata_returnsContextUnchanged() {
    var context = new PromptContext("hello", Map.of());
    PromptContext result = interceptor.intercept(context);
    assertSame(context, result);
  }

  @ParameterizedTest
  @CsvSource({
    "locale, fr, French",
    "locale, fr_CA, French",
    "language, es, Spanish",
    "locale, de, German",
    "locale, pt, Portuguese",
    "locale, it, Italian",
    "locale, nl, Dutch",
    "locale, ja, Japanese",
    "locale, ko, Korean",
    "locale, zh, Chinese",
    "locale, ar, Arabic",
    "locale, ru, Russian"
  })
  void intercept_supportedLocales_normalizeToLanguageName(
      String key, String value, String expectedLanguageName) {
    var context = new PromptContext("test", Map.of(key, value));
    PromptContext result = interceptor.intercept(context);
    String directive = (String) result.systemMetadata().get("languageAlignmentDirective");
    assertNotNull(directive);
    assertTrue(directive.contains(expectedLanguageName));
  }

  @Test
  void intercept_unknownLocale_usesRawTag() {
    var context = new PromptContext("test", Map.of("locale", "sw"));
    PromptContext result = interceptor.intercept(context);

    String directive = (String) result.systemMetadata().get("languageAlignmentDirective");
    assertTrue(directive.contains("sw"));
  }

  @Test
  void intercept_localeWithDash_splitsCorrectly() {
    var context = new PromptContext("test", Map.of("locale", "de-DE"));
    PromptContext result = interceptor.intercept(context);

    String directive = (String) result.systemMetadata().get("languageAlignmentDirective");
    assertTrue(directive.contains("German"));
  }

  @Test
  void intercept_preservesExistingSystemMetadata() {
    var context =
        new PromptContext(
            "test",
            Map.of("locale", "fr"),
            Map.of("existingKey", "existingValue"),
            "test",
            null,
            RoutingMode.DETERMINISTIC);
    PromptContext result = interceptor.intercept(context);

    assertEquals("existingValue", result.systemMetadata().get("existingKey"));
    assertTrue(result.systemMetadata().containsKey("languageAlignmentDirective"));
  }
}
