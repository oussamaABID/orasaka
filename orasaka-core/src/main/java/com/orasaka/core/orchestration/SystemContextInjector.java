package com.orasaka.core.orchestration;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 2: System Context Injector Interceptor.
 *
 * <p>Loops through all registered IoC-provided {@link SystemContextProvider} beans to inject
 * ambient system data.
 */
@Component
public class SystemContextInjector implements PromptInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(SystemContextInjector.class);

  private final List<SystemContextProvider> providers;

  /**
   * Constructs the injector with registered system context providers.
   *
   * @param providers The list of context providers.
   */
  public SystemContextInjector(List<SystemContextProvider> providers) {
    this.providers = providers != null ? providers : List.of();
  }

  @Override
  public void intercept(PromptContext context) {
    logger.debug("Injecting system context signals. Provider count: {}", providers.size());
    providers.forEach(
        provider -> {
          try {
            Map<String, Object> data = provider.getSystemContext();
            if (data != null) {
              context.systemMetadata().putAll(data);
            }
          } catch (Exception e) {
            logger.error(
                "Error invoking SystemContextProvider: {}", provider.getClass().getName(), e);
          }
        });
  }

  @Override
  public int getOrder() {
    return 2;
  }
}
