package com.orasaka.persistence.identity.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Clean domain DTO representing an AiMcpServer, satisfying ERR-106. */
public record AiMcpServerDto(
    Integer id, String userId, String name, String url, Boolean enabled, Instant createdAt)
    implements Serializable {

  public AiMcpServerDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(url, "url cannot be null");
    if (enabled == null) {
      enabled = Boolean.TRUE;
    }
  }
}
