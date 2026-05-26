package com.orasaka.persistence.application.service;

import com.orasaka.persistence.domain.model.ChatSessionDto;
import com.orasaka.persistence.infrastructure.adapter.persistence.entity.ChatSessionEntity;

/** Package-private final mapper utility for chat sessions. Follows ERR-107. */
final class ChatSessionMapper {

  private ChatSessionMapper() {}

  static ChatSessionEntity toEntity(ChatSessionDto dto) {
    if (dto == null) {
      return null;
    }
    ChatSessionEntity entity = new ChatSessionEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setTitle(dto.title());
    entity.setUpdatedAt(dto.updatedAt());
    return entity;
  }

  static ChatSessionDto toDto(ChatSessionEntity entity) {
    if (entity == null) {
      return null;
    }
    return new ChatSessionDto(
        entity.getId(), entity.getUserId(), entity.getTitle(), entity.getUpdatedAt());
  }
}
