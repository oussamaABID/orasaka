package com.orasaka.gateway.infrastructure.support;

/**
 * Custom runtime exception thrown when the message broker queue is overloaded or when a broker
 * outage occurs (triggered by Resilience4j fallback).
 */
public class SystemOverloadedException extends RuntimeException {

  public SystemOverloadedException(String message) {
    super(message);
  }

  public SystemOverloadedException(String message, Throwable cause) {
    super(message, cause);
  }
}
