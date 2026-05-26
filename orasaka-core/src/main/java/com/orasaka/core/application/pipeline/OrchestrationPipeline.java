package com.orasaka.core.application.pipeline;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Coordination manager for the prompt interceptor pipeline chain of responsibility.
 *
 * <p>Supports dynamic ordering from the database via {@link PipelineConfigProvider}. Falls back to
 * hardcoded {@code getOrder()} sorting when the database is empty or unreachable.
 */
@Component
public class OrchestrationPipeline {

  private static final Logger logger = LoggerFactory.getLogger(OrchestrationPipeline.class);

  private final Map<String, PromptContextInterceptor> interceptorRegistry;
  private final List<PromptContextInterceptor> fallbackChain;
  private final CoreProperties properties;
  private final PipelineConfigProvider configProvider;

  private final AtomicReference<List<PromptContextInterceptor>> cachedChain =
      new AtomicReference<>();

  /**
   * Constructs the orchestration pipeline.
   *
   * @param interceptors Registered PromptInterceptor list.
   * @param properties Configuration properties.
   * @param configProvider Database-driven pipeline config provider.
   */
  public OrchestrationPipeline(
      List<PromptContextInterceptor> interceptors,
      CoreProperties properties,
      PipelineConfigProvider configProvider) {
    List<PromptContextInterceptor> all =
        new ArrayList<>(interceptors != null ? interceptors : List.of());

    // Build registry keyed by simple class name
    this.interceptorRegistry =
        all.stream()
            .collect(Collectors.toMap(i -> i.getClass().getSimpleName(), i -> i, (a, b) -> a));

    // Fallback: sorted by hardcoded getOrder()
    all.sort(Comparator.comparingInt(PromptContextInterceptor::getOrder));
    this.fallbackChain = List.copyOf(all);

    this.properties = properties;
    this.configProvider = configProvider;

    logger.info(
        "Initialized Prompt Orchestration Pipeline with {} registered interceptors.",
        this.interceptorRegistry.size());
  }

  /**
   * Executes the interceptor pipeline chain on the raw user input query.
   *
   * @param rawUserQuery The original raw prompt.
   * @param context The execution context.
   * @return The enriched PromptContext, or null if the pipeline is disabled.
   */
  public PromptContext process(String rawUserQuery, Context context) {
    boolean enabled = properties.orchestration() != null && properties.orchestration().enabled();
    if (!enabled) {
      return null;
    }

    logger.debug("Executing prompt matrix orchestration chain...");
    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put("userId", context.userId());
    userMetadata.put("conversationId", context.conversationId());
    userMetadata.putAll(context.preferences());
    userMetadata.put("roles", context.authorities().stream().map(Authority::name).toList());

    PromptContext initialContext = new PromptContext(rawUserQuery, userMetadata);
    List<PromptContextInterceptor> chain = resolveChain();

    return chain.stream()
        .reduce(
            initialContext,
            (ctx, interceptor) -> {
              try {
                return interceptor.intercept(ctx);
              } catch (Exception e) {
                logger.error(
                    "Error executing prompt interceptor '{}'", interceptor.getClass().getName(), e);
                return ctx;
              }
            },
            (c1, c2) -> c1);
  }

  /**
   * Evicts the cached chain, forcing a rebuild from the database on next {@code process()} call.
   * Called by admin controllers after configuration changes.
   */
  public void evictChainCache() {
    cachedChain.set(null);
    logger.info("Pipeline chain cache evicted — next execution will rebuild from database.");
  }

  /**
   * Returns the current chain configuration as domain records.
   *
   * @return Ordered list of interceptor configs reflecting the current chain state.
   */
  public List<InterceptorConfig> getCurrentConfig() {
    try {
      List<InterceptorConfig> dbConfigs = configProvider.findAllOrdered();
      if (!dbConfigs.isEmpty()) {
        return dbConfigs;
      }
    } catch (Exception e) {
      logger.warn("Failed to load pipeline config from database — returning defaults.", e);
    }
    return buildDefaultConfigs();
  }

  /**
   * Builds default interceptor configurations from the hardcoded fallback chain.
   *
   * @return List of default interceptor configs.
   */
  public List<InterceptorConfig> buildDefaultConfigs() {
    return fallbackChain.stream()
        .map(
            i ->
                new InterceptorConfig(
                    i.getClass().getSimpleName(),
                    humanize(i.getClass().getSimpleName()),
                    i.getOrder(),
                    true,
                    ""))
        .toList();
  }

  /**
   * Resolves the active interceptor chain. Uses cached chain if available, otherwise rebuilds from
   * the database with fallback to hardcoded ordering.
   */
  private List<PromptContextInterceptor> resolveChain() {
    List<PromptContextInterceptor> chain = cachedChain.get();
    if (chain != null) {
      return chain;
    }

    try {
      List<InterceptorConfig> dbConfigs = configProvider.findAllOrdered();
      if (!dbConfigs.isEmpty()) {
        chain = buildChainFromConfigs(dbConfigs);
        cachedChain.set(chain);
        logger.debug(
            "Pipeline chain rebuilt from database ({} active interceptors).", chain.size());
        return chain;
      }
    } catch (Exception e) {
      logger.warn("Failed to load pipeline config from database — using fallback chain.", e);
    }

    cachedChain.set(fallbackChain);
    return fallbackChain;
  }

  /**
   * Builds an ordered chain from database configurations, resolving each key to a registered bean.
   */
  private List<PromptContextInterceptor> buildChainFromConfigs(List<InterceptorConfig> configs) {
    List<PromptContextInterceptor> chain = new ArrayList<>();
    for (InterceptorConfig config : configs) {
      if (!config.enabled()) {
        logger.debug("Interceptor '{}' is disabled — skipping.", config.interceptorKey());
        continue;
      }
      PromptContextInterceptor interceptor = interceptorRegistry.get(config.interceptorKey());
      if (interceptor != null) {
        chain.add(interceptor);
      } else {
        logger.warn(
            "Interceptor key '{}' from database has no registered bean — skipping.",
            config.interceptorKey());
      }
    }
    return List.copyOf(chain);
  }

  /**
   * Converts a PascalCase class name into a human-readable label.
   *
   * @param className The simple class name.
   * @return A space-separated human-readable string.
   */
  private static String humanize(String className) {
    // Safe split: insert space before each uppercase letter that follows a lowercase
    StringBuilder sb = new StringBuilder(className.length() + 8);
    for (int i = 0; i < className.length(); i++) {
      char c = className.charAt(i);
      if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(className.charAt(i - 1))) {
        sb.append(' ');
      }
      sb.append(c);
    }
    return sb.toString();
  }
}
