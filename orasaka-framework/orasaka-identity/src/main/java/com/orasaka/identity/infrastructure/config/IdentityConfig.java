package com.orasaka.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestClient;

/**
 * Spring {@link Configuration} for the {@code orasaka-identity} module.
 *
 * <p>Declares infrastructure beans shared across identity services, starting with the {@link
 * BCryptPasswordEncoder} which must be a singleton to benefit from BCrypt's intentional
 * computational cost (re-instantiating it per-call wastes CPU).
 *
 * <p>This class deliberately avoids auto-configuration starters ({@code
 * spring-boot-starter-security} auto-configuration) to remain compliant with AGENTS.md §1.A — No
 * Starter Leaks.
 *
 * @see com.orasaka.identity.service.IdentityService
 */
@Configuration
@ConfigurationPropertiesScan
public class IdentityConfig {

  /**
   * Declares a singleton {@link BCryptPasswordEncoder} bean for the identity module.
   *
   * <p>Using the default strength (10 rounds). This bean is injected into {@link
   * com.orasaka.identity.service.IdentityService} for password verification and can be reused by
   * any future authentication component without creating duplicate instances.
   *
   * <p>BCrypt is intentionally slow by design — sharing a single instance ensures the cost is paid
   * only for actual password operations, not for encoder construction.
   *
   * @return A configured {@link BCryptPasswordEncoder} with default work factor (strength 10).
   * @see com.orasaka.identity.service.IdentityService#authenticate(String, String)
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Default {@link RestClient} bean for identity HTTP operations.
   *
   * <p>Used by OAuth2 provider verifiers (GitHub, Google) for token validation calls. Per §2.16
   * [ERR-120], all HTTP calls use Spring {@link RestClient}.
   *
   * @return A default {@link RestClient} instance.
   */
  @Bean
  public RestClient identityRestClient() {
    return RestClient.create();
  }
}
