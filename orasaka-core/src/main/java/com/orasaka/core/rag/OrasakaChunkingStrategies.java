package com.orasaka.core.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/** Pre-built, reusable chunking strategies library. */
public enum OrasakaChunkingStrategies implements OrasakaChunker {
  PLAIN_TEXT {
    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      List<Document> docs = new ArrayList<>();
      // Split by paragraph
      String[] paragraphs = content.split("\\n\\n+");
      for (String paragraph : paragraphs) {
        String trimmed = paragraph.trim();
        if (!trimmed.isEmpty()) {
          docs.add(new Document(trimmed, new HashMap<>(metadata)));
        }
      }
      return docs;
    }
  },

  MARKDOWN_CHUNKERS {
    @Override
    public List<Document> chunk(String content, Map<String, Object> metadata) {
      if (content == null || content.isBlank()) {
        return Collections.emptyList();
      }
      List<Document> docs = new ArrayList<>();
      // Split by markdown headers
      String[] sections = content.split("(?m)^(?=#{1,6}\\s+)");
      for (String section : sections) {
        String trimmed = section.trim();
        if (!trimmed.isEmpty()) {
          docs.add(new Document(trimmed, new HashMap<>(metadata)));
        }
      }
      return docs;
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
        List<Document> docs = new ArrayList<>();
        for (Object item : list) {
          if (item == null) continue;
          String docText;
          Map<String, Object> itemMeta = new HashMap<>(metadata);
          if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapItem = (Map<String, Object>) item;
            docText = jsonMapper.writeValueAsString(mapItem);
            // Merge map values into metadata as well for richer search options
            mapItem.forEach(
                (k, v) -> {
                  if (v != null) {
                    itemMeta.put(k, v);
                  }
                });
          } else {
            docText = item.toString();
          }
          docs.add(new Document(docText, itemMeta));
        }
        return docs;
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
