package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

/** Embeddable composite primary key record for {@link UserInterceptionEntity}. */
@Embeddable
public record UserInterceptionId(
    @Column(name = "user_id", length = 255) String userId,
    @Column(name = "interception_type", length = 100) String interceptionType)
    implements Serializable {

  private static final long serialVersionUID = 1L;
}
