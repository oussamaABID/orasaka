package com.orasaka.core.interceptors.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.document.Document;

/** Pre-built, reusable chunking strategies library. */
public enum OrasakaChunkingStrategies implements OrasakaChunker {
  PLAIN_TEXT {
    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      return java.util.Arrays.stream(content.split("\\n\\n+"))
          .map(String::trim)
          .filter(trimmed -> !trimmed.isEmpty())
          .map(trimmed -> new Document(trimmed, new HashMap<>(metadata)))
          .toList();
    }
  },

  MARKDOWN_CHUNKERS {
    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      return java.util.Arrays.stream(content.split("(?m)^(?=#{1,6}\\s+)"))
          .map(String::trim)
          .filter(trimmed -> !trimmed.isEmpty())
          .map(trimmed -> new Document(trimmed, new HashMap<>(metadata)))
          .toList();
    }
  },

  JSON_ARRAY {
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      try {
        List<Object> list = jsonMapper.readValue(content, new TypeReference<List<Object>>() {});
        return list.stream()
            .filter(java.util.Objects::nonNull)
            .map(
                item -> {
                  Map<String, Object> itemMeta = new HashMap<>(metadata);
                  String docText;
                  if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapItem = (Map<String, Object>) item;
                    try {
                      docText = jsonMapper.writeValueAsString(mapItem);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                    mapItem.forEach(
                        (k, v) -> Optional.ofNullable(v).ifPresent(val -> itemMeta.put(k, val)));
                  } else {
                    docText = item.toString();
                  }
                  return new Document(docText, itemMeta);
                })
            .toList();
      } catch (Exception e) {
        // Fallback to plain text paragraph chunking if JSON parsing fails
        return PLAIN_TEXT.chunk(content, metadata);
      }
    }
  };

  /**
   * Resolves a strategy by its configuration token name.
   *
   * @param strategyName The configuration token name.
   * @return The matched chunker strategy, defaulting to PLAIN_TEXT if unmatched.
   */
  public static OrasakaChunker resolve(String strategyName) {
    if (strategyName == null) {
      return PLAIN_TEXT;
    }
    try {
      return valueOf(strategyName.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      return PLAIN_TEXT;
    }
  }
}
