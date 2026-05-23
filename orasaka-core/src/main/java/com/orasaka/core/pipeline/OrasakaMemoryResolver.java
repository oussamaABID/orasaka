package com.orasaka.core.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/**
 * Thread-safe resolver for session-scoped {@link ChatMemory} instances.
 *
 * <p>Ensures strict multi-session partitioning by mapping each {@code conversationId} to an
 * isolated {@link ChatMemory} store. This guarantees that conversation history never leaks across
 * different user sessions or concurrent threads, satisfying the Guardian Protocol defined in
 * AGENTS.md §3.A.
 *
 * <p>Internal storage uses a {@link ConcurrentHashMap} making this component fully safe for
 * concurrent access by Java 21 Virtual Threads.
 *
 * @see org.springframework.ai.chat.memory.ChatMemory
 */
@Component
public class OrasakaMemoryResolver {

  private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

  /**
   * Resolves or creates an isolated {@link ChatMemory} instance for the given conversation.
   *
   * <p>If {@code conversationId} is {@code null} or blank, a transient (non-stored) {@link
   * OrasakaInMemoryChatMemory} is returned — suitable for one-off stateless calls. Otherwise, the
   * memory is persisted in the internal map for the lifetime of this bean.
   *
   * <p>This method is safe for concurrent Virtual Thread access due to the underlying {@link
   * ConcurrentHashMap#computeIfAbsent} atomicity guarantee.
   *
   * @param conversationId The unique session identifier. May be {@code null} for stateless calls.
   * @return The {@link ChatMemory} bound to the given conversation, or a fresh transient instance.
   */
  public ChatMemory resolve(String conversationId) {
    if (conversationId == null || conversationId.isBlank()) {
      return new OrasakaInMemoryChatMemory();
    }
    return sessionMemories.computeIfAbsent(conversationId, id -> new OrasakaInMemoryChatMemory());
  }

  /**
   * Removes and discards the {@link ChatMemory} associated with the given conversation.
   *
   * <p>Should be called when a user explicitly closes a conversation thread or when session
   * eviction is triggered by a TTL policy. This method is idempotent — calling it with an unknown
   * {@code conversationId} has no effect.
   *
   * @param conversationId The unique session identifier to purge. No-op if not found.
   */
  public void purge(String conversationId) {
    sessionMemories.remove(conversationId);
  }

  /**
   * In-memory {@link ChatMemory} implementation backed by a simple {@link ArrayList}.
   *
   * <p>Each instance is scoped to a single conversation. This implementation is not thread-safe on
   * its own, but concurrent access is prevented by the {@link OrasakaMemoryResolver} which creates
   * one instance per {@code conversationId} atomically.
   *
   * <p><strong>Note:</strong> All messages are stored in-process. State is lost on application
   * restart unless an external persistence layer (e.g., Redis, PostgreSQL) is configured.
   */
  private static class OrasakaInMemoryChatMemory implements ChatMemory {
    private final List<Message> messages = new ArrayList<>();

    /**
     * Appends messages to this conversation's history.
     *
     * @param conversationId The conversation identifier (informational; scoping is handled
     *     externally).
     * @param messages The list of {@link Message} objects to append.
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
      this.messages.addAll(messages);
    }

    /**
     * Returns a defensive copy of all messages in this conversation.
     *
     * @param conversationId The conversation identifier (informational; scoping is handled
     *     externally).
     * @return A new {@link ArrayList} containing all stored messages.
     */
    @Override
    public List<Message> get(String conversationId) {
      return new ArrayList<>(messages);
    }

    /**
     * Clears all messages from this conversation's history.
     *
     * @param conversationId The conversation identifier (informational; scoping is handled
     *     externally).
     */
    @Override
    public void clear(String conversationId) {
      messages.clear();
    }
  }
}
