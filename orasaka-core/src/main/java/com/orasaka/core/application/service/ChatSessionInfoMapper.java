package com.orasaka.core.application.service;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.persistence.domain.model.ChatSessionDto;

/**
 * Package-private final mapper utility for mapping ChatSessionDto to ChatSessionInfo. Follows
 * ERR-107.
 */
final class ChatSessionInfoMapper {

  private ChatSessionInfoMapper() {}

  static ChatSessionInfo toInfo(ChatSessionDto dto) {
    if (dto == null) {
      return null;
    }
    return new ChatSessionInfo(dto.id(), dto.userId(), dto.title(), dto.updatedAt());
  }

  static ChatSessionDto toDto(ChatSessionInfo info) {
    if (info == null) {
      return null;
    }
    return new ChatSessionDto(info.id(), info.userId(), info.title(), info.updatedAt());
  }
}
