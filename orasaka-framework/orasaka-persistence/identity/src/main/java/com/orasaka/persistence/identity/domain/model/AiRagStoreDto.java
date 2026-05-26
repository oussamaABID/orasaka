package com.orasaka.persistence.identity.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Clean domain DTO representing an AiRagStore, satisfying ERR-106. */
public record AiRagStoreDto(
    Integer id,
    String userId,
    String name,
    String storeType,
    String host,
    Integer port,
    String databaseName,
    String tableName,
    String username,
    String password,
    Boolean enabled,
    Instant createdAt)
    implements Serializable {

  public AiRagStoreDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(storeType, "storeType cannot be null");
    if (enabled == null) {
      enabled = Boolean.TRUE;
    }
  }
}
