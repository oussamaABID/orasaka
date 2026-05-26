package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.core.domain.ports.inbound.ChatSessionService;
import com.orasaka.identity.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** REST controller managing user memory blocks / chat sessions. */
@RestController
@RequestMapping("/api/v1/chats")
public class ChatSessionController {

  private final ChatSessionService chatSessionService;

  public ChatSessionController(ChatSessionService chatSessionService) {
    this.chatSessionService =
        Objects.requireNonNull(chatSessionService, "ChatSessionService must not be null");
  }

  @GetMapping
  public ResponseEntity<List<ChatSessionInfo>> getSessions(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(chatSessionService.getSessionsByUserId(user.id().toString()));
  }

  @PostMapping
  public ResponseEntity<ChatSessionInfo> createSession(
      @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
    String id = body.get("conversationId");
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
    String title = body.getOrDefault("title", "New Memory Block");
    ChatSessionInfo session = new ChatSessionInfo(id, user.id().toString(), title, Instant.now());
    return ResponseEntity.ok(chatSessionService.save(session));
  }

  @PatchMapping("/{sessionId}")
  public ResponseEntity<ChatSessionInfo> renameSession(
      @PathVariable String sessionId,
      @RequestBody Map<String, String> body,
      @AuthenticationPrincipal User user) {
    String title = body.get("title");
    if (title == null || title.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return chatSessionService
        .getSession(sessionId)
        .map(
            session -> {
              ChatSessionInfo updated =
                  new ChatSessionInfo(session.id(), session.userId(), title, Instant.now());
              return ResponseEntity.ok(chatSessionService.save(updated));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> deleteSession(
      @PathVariable String sessionId, @AuthenticationPrincipal User user) {
    chatSessionService.deleteSession(sessionId);
    return ResponseEntity.ok().build();
  }
}
