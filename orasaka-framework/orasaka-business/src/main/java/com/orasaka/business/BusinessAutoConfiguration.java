package com.orasaka.business;

import com.orasaka.business.prompt.MarkdownPromptResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Spring Boot auto-configuration for the orasaka-business module.
 *
 * <p>Registers the {@link MarkdownPromptResolver} bean, making Git-tracked prompt templates
 * available to any module that depends on {@code orasaka-business}.
 *
 * <p>Discovered at runtime via {@code META-INF/spring/AutoConfiguration.imports} — zero classpath
 * scanning.
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class BusinessAutoConfiguration {

  @Bean
  MarkdownPromptResolver markdownPromptResolver(ResourceLoader resourceLoader) {
    return new MarkdownPromptResolver(resourceLoader);
  }
}
