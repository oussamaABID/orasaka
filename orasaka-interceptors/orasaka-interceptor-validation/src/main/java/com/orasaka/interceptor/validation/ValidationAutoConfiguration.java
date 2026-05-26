package com.orasaka.interceptor.validation;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the validation interceptor module.
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class ValidationAutoConfiguration {

  /**
   * Registers the {@link CostShieldInterceptor} as a Spring bean.
   *
   * @return The cost shield interceptor instance.
   */
  @Bean
  public CostShieldInterceptor costShieldInterceptor() {
    return new CostShieldInterceptor();
  }
}
