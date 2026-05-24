package com.orasaka.core.pipeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      return Arrays.stream(content.split("\\n\\n+"))
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
      return Arrays.stream(content.split("(?m)^(?=#{1,6}\\s+)"))
          .map(String::trim)
          .filter(trimmed -> !trimmed.isEmpty())
          .map(trimmed -> new Document(trimmed, new HashMap<>(metadata)))
          .toList();
    }
  },

  JSON_ARRAY {
    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      return JsonChunkMapper.parseJsonArray(content)
          .map(list -> list.stream().filter(Objects::nonNull).map(item -> JsonChunkMapper.toDocument(item, metadata)).toList())
          .orElseGet(() -> PLAIN_TEXT.chunk(content, metadata));
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

/** Package-private JSON serialization utility for chunking pipeline. */
final class JsonChunkMapper {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private JsonChunkMapper() {}

  static Optional<List<Object>> parseJsonArray(String content) {
    try {
      return Optional.of(JSON_MAPPER.readValue(content, new TypeReference<List<Object>>() {}));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  static Document toDocument(Object item, Map<String, Object> metadata) {
    Map<String, Object> itemMeta = new HashMap<>(metadata);
    String docText = serializeItem(item, itemMeta);
    return new Document(docText, itemMeta);
  }

  @SuppressWarnings("unchecked")
  private static String serializeItem(Object item, Map<String, Object> itemMeta) {
    if (!(item instanceof Map)) {
      return item.toString();
    }
    Map<String, Object> mapItem = (Map<String, Object>) item;
    mapItem.forEach((k, v) -> Optional.ofNullable(v).ifPresent(val -> itemMeta.put(k, val)));
    return writeJson(mapItem).orElseGet(mapItem::toString);
  }

  private static Optional<String> writeJson(Object value) {
    try {
      return Optional.of(JSON_MAPPER.writeValueAsString(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
