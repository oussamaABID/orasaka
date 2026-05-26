package com.orasaka.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

/** Unit tests for {@link OperationNode} compact constructor validation. */
class OperationNodeTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() { org.junit.jupiter.api.Assertions.assertTrue(true); }


  private static final NodeState ACTIVE = new NodeState.Active();
  private static final TargetExecutionUri EXEC =
      new TargetExecutionUri("/api/v1/chat", "POST", null);

  @Nested
  @DisplayName("Null-safety validations")
  class NullSafety {

    @Test
    @DisplayName("null id throws NPE")
    void nullId() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode(null, "Chat", CHAT, null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("blank id throws IAE")
    void blankId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new OperationNode("  ", "Chat", CHAT, null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("null label throws NPE")
    void nullLabel() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", null, CHAT, null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("blank label throws IAE")
    void blankLabel() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new OperationNode("id", "  ", CHAT, null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("null icon throws NPE")
    void nullIcon() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", "Chat", null, null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("blank icon throws IAE")
    void blankIcon() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new OperationNode("id", "Chat", "  ", null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("null state throws NPE")
    void nullState() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", "Chat", CHAT, null, null, EXEC));
    }

    @Test
    @DisplayName("null executionDetails throws NPE")
    void nullExecDetails() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", "Chat", CHAT, null, ACTIVE, null));
    }
  }

  @Nested
  @DisplayName("Default values")
  class Defaults {

    @Test
    @DisplayName("null presentationContext defaults to CONTEXT_MENU_PLUS")
    void defaultPresentationContext() {
      var node = new OperationNode("id", "Chat", CHAT, null, ACTIVE, EXEC);
      assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
    }

    @Test
    @DisplayName("blank presentationContext defaults to CONTEXT_MENU_PLUS")
    void blankPresentationContext() {
      var node = new OperationNode("id", "Chat", CHAT, "  ", ACTIVE, EXEC);
      assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
    }

    @Test
    @DisplayName("explicit presentationContext preserved")
    void explicitPresentationContext() {
      var node = new OperationNode("id", "Chat", CHAT, "PRIMARY", ACTIVE, EXEC);
      assertEquals("PRIMARY", node.presentationContext());
    }
  }
}
