package com.orasaka.core.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Order 2 — Loops over active {@link SystemContextProvider} implementations to feed real-time
 * environment signals, system variables, and marketplace data into the {@link PromptContext}'s
 * system metadata via IoC.
 *
 * @see SystemContextProvider
 * @since 1.0.0
 */
@Component
class SystemContextInjector implements PromptInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(SystemContextInjector.class);
  private final List<SystemContextProvider> providers;

  /**
   * @param providers The ordered list of system context providers (nullable — defaults to empty).
   */
  public SystemContextInjector(List<SystemContextProvider> providers) {
    this.providers = List.copyOf(Objects.requireNonNullElse(providers, List.of()));
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Injecting system context signals. Provider count: {}", providers.size());
    var newSystemMetadata = new HashMap<>(context.systemMetadata());

    for (SystemContextProvider provider : providers) {
      try {
        Map<String, Object> data = provider.getSystemContext();
        if (data != null) newSystemMetadata.putAll(data);
      } catch (Exception e) {
        logger.error("Error invoking SystemContextProvider: {}", provider.getClass().getName(), e);
      }
    }
    return context.withSystemMetadata(Map.copyOf(newSystemMetadata));
  }

  @Override
  public int getOrder() {
    return 2;
  }
}
