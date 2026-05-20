package com.orasaka.core.engine;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe resolver for session-based ChatMemory.
 * Ensures strict multi-session partitioning using conversationId.
 */
@Component
public class OrasakaMemoryResolver {

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    public ChatMemory resolve(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return new OrasakaInMemoryChatMemory();
        }
        return sessionMemories.computeIfAbsent(conversationId, id -> new OrasakaInMemoryChatMemory());
    }

    public void purge(String conversationId) {
        sessionMemories.remove(conversationId);
    }

    private static class OrasakaInMemoryChatMemory implements ChatMemory {
        private final List<Message> messages = new ArrayList<>();

        @Override
        public void add(String conversationId, List<Message> messages) {
            this.messages.addAll(messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            return new ArrayList<>(messages);
        }

        @Override
        public void clear(String conversationId) {
            messages.clear();
        }
    }
}
