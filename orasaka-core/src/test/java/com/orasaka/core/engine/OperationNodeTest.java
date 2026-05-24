package com.orasaka.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OperationNode} compact constructor validation. */
class OperationNodeTest {

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
          () -> new OperationNode(null, "Chat", "chat", null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("blank id throws IAE")
    void blankId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new OperationNode("  ", "Chat", "chat", null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("null label throws NPE")
    void nullLabel() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", null, "chat", null, ACTIVE, EXEC));
    }

    @Test
    @DisplayName("blank label throws IAE")
    void blankLabel() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new OperationNode("id", "  ", "chat", null, ACTIVE, EXEC));
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
          () -> new OperationNode("id", "Chat", "chat", null, null, EXEC));
    }

    @Test
    @DisplayName("null executionDetails throws NPE")
    void nullExecDetails() {
      assertThrows(
          NullPointerException.class,
          () -> new OperationNode("id", "Chat", "chat", null, ACTIVE, null));
    }
  }

  @Nested
  @DisplayName("Default values")
  class Defaults {

    @Test
    @DisplayName("null presentationContext defaults to CONTEXT_MENU_PLUS")
    void defaultPresentationContext() {
      var node = new OperationNode("id", "Chat", "chat", null, ACTIVE, EXEC);
      assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
    }

    @Test
    @DisplayName("blank presentationContext defaults to CONTEXT_MENU_PLUS")
    void blankPresentationContext() {
      var node = new OperationNode("id", "Chat", "chat", "  ", ACTIVE, EXEC);
      assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
    }

    @Test
    @DisplayName("explicit presentationContext preserved")
    void explicitPresentationContext() {
      var node = new OperationNode("id", "Chat", "chat", "PRIMARY", ACTIVE, EXEC);
      assertEquals("PRIMARY", node.presentationContext());
    }
  }
}
