package com.orasaka.gateway.infrastructure.adapter.rest;

import java.util.Objects;

/** DTO record representing the configured state of a user's API credential. */
public record UserCredentialResponse(String providerName, boolean configured) {

  public UserCredentialResponse {
    Objects.requireNonNull(providerName, "providerName must not be null");
    if (providerName.isBlank()) {
      throw new IllegalArgumentException("providerName must not be blank");
    }
  }
}
