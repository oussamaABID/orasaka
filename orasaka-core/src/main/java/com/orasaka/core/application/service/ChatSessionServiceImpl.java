package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.core.domain.ports.inbound.ChatSessionService;
import com.orasaka.persistence.domain.ports.inbound.ChatSessionPersistenceProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Package-private implementation of the ChatSessionService inbound port. Delegates to persistence.
 * Follows ERR-105.
 */
@Service
class ChatSessionServiceImpl implements ChatSessionService {

  private final ChatSessionPersistenceProvider provider;

  ChatSessionServiceImpl(ChatSessionPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "ChatSessionPersistenceProvider cannot be null");
  }

  @Override
  public ChatSessionInfo save(ChatSessionInfo session) {
    return ChatSessionInfoMapper.toInfo(provider.save(ChatSessionInfoMapper.toDto(session)));
  }

  @Override
  public Optional<ChatSessionInfo> getSession(String id) {
    return provider.findById(id).map(ChatSessionInfoMapper::toInfo);
  }

  @Override
  public List<ChatSessionInfo> getSessionsByUserId(String userId) {
    return provider.findAllByUserId(userId).stream().map(ChatSessionInfoMapper::toInfo).toList();
  }

  @Override
  public void deleteSession(String id) {
    provider.deleteById(id);
  }

  @Override
  public void purgeSessionsByUserId(String userId) {
    provider.purgeByUserId(userId);
  }
}
