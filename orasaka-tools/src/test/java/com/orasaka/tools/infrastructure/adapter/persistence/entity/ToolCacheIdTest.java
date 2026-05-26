package com.orasaka.tools.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ToolCacheId} — composite key equals/hashCode contract. */
class ToolCacheIdTest {

  @Nested
  @DisplayName("Equality contract")
  class EqualityContract {

    @Test
    @DisplayName("same fields are equal")
    void sameFieldsEqual() {
      var id1 = new ToolCacheId("tool1", "key1");
      var id2 = new ToolCacheId("tool1", "key1");
      assertEquals(id1, id2);
      assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("different toolId not equal")
    void differentToolId() {
      var id1 = new ToolCacheId("tool1", "key1");
      var id2 = new ToolCacheId("tool2", "key1");
      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("different cacheKey not equal")
    void differentCacheKey() {
      var id1 = new ToolCacheId("tool1", "key1");
      var id2 = new ToolCacheId("tool1", "key2");
      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      var id = new ToolCacheId("tool1", "key1");
      assertNotEquals(null, id);
    }

    @Test
    @DisplayName("reflexive equality")
    void reflexive() {
      var id1 = new ToolCacheId("tool1", "key1");
      var id2 = new ToolCacheId("tool1", "key1");
      assertEquals(id1, id2, "Equal objects must be equal");
    }
  }

  @Nested
  @DisplayName("Getters and setters")
  class GettersSetters {

    @Test
    @DisplayName("default constructor and setters")
    void defaultConstructor() {
      var id = new ToolCacheId();
      id.setToolId("t1");
      id.setCacheKey("k1");
      assertEquals("t1", id.getToolId());
      assertEquals("k1", id.getCacheKey());
    }
  }
}
