package com.orasaka.core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security governance configuration properties. Maps to {@code orasaka.security} in {@code
 * application.yml}.
 *
 * <p>The {@code disableAi} property acts as a hard governance kill-switch. When set to {@code
 * true}, the {@code DynamicPipelineExecutor} will throw a {@link SecurityException} if any
 * interceptor returning {@code isAiDependent() == true} is invoked.
 *
 * @param disableAi When {@code true}, blocks all AI-dependent interceptors with a hard {@link
 *     SecurityException}. This is a compliance shield, not a performance failover.
 */
@ConfigurationProperties(prefix = "orasaka.security")
public record SecurityProperties(boolean disableAi) {

  /** Default constructor — AI is enabled by default. */
  public SecurityProperties() {
    this(false);
  }
}
