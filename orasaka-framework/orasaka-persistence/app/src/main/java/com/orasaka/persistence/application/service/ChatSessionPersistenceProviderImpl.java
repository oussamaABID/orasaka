package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.ChatSessionDto;
import com.orasaka.persistence.domain.ports.inbound.ChatSessionPersistenceProvider;
import com.orasaka.persistence.infrastructure.adapter.persistence.repository.ChatSessionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ChatSessionPersistenceProviderImpl implements ChatSessionPersistenceProvider {

  private final ChatSessionRepository repository;

  ChatSessionPersistenceProviderImpl(ChatSessionRepository repository) {
    this.repository = Objects.requireNonNull(repository, "ChatSessionRepository cannot be null");
  }

  @Override
  public ChatSessionDto save(ChatSessionDto session) {
    var entity = ChatSessionMapper.toEntity(session);
    var saved = repository.save(entity);
    return ChatSessionMapper.toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSessionDto> findById(String id) {
    return repository.findById(id).map(ChatSessionMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatSessionDto> findAllByUserId(String userId) {
    return repository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
        .map(ChatSessionMapper::toDto)
        .toList();
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Override
  public void purgeByUserId(String userId) {
    repository.deleteAllByUserId(userId);
  }
}
