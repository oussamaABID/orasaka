package com.orasaka.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OperationNodeTest {

  private final TargetExecutionUri uri = new TargetExecutionUri("/api", "POST", null);
  private final NodeState state = new NodeState.Active();

  @Test
  void validConstruction() {
    var node = new OperationNode("chat.image", "Image Gen", "image", "TOOLBAR", state, uri);
    assertEquals("chat.image", node.id());
    assertEquals("Image Gen", node.label());
    assertEquals("image", node.icon());
    assertEquals("TOOLBAR", node.presentationContext());
    assertEquals(state, node.state());
    assertEquals(uri, node.executionDetails());
  }

  @Test
  void nullPresentationContext_defaultsToContextMenuPlus() {
    var node = new OperationNode("id", "label", "icon", null, state, uri);
    assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
  }

  @Test
  void blankPresentationContext_defaultsToContextMenuPlus() {
    var node = new OperationNode("id", "label", "icon", "  ", state, uri);
    assertEquals("CONTEXT_MENU_PLUS", node.presentationContext());
  }

  @Test
  void nullId_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new OperationNode(null, "label", "icon", null, state, uri));
  }

  @Test
  void blankId_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OperationNode("  ", "label", "icon", null, state, uri));
  }

  @Test
  void nullLabel_throws() {
    assertThrows(
        NullPointerException.class, () -> new OperationNode("id", null, "icon", null, state, uri));
  }

  @Test
  void nullState_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new OperationNode("id", "label", "icon", null, null, uri));
  }

  @Test
  void nullExecutionDetails_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new OperationNode("id", "label", "icon", null, state, null));
  }
}
