package com.orasaka.core.application.pipeline;

import com.orasaka.core.application.interceptor.PromptContextInterceptor;
import com.orasaka.core.domain.model.Authority;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.InterceptorConfig;
import com.orasaka.core.domain.model.PromptContext;
import com.orasaka.core.domain.model.RoutingMode;
import com.orasaka.core.domain.ports.outbound.PipelineConfigProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import com.orasaka.core.infrastructure.config.SecurityProperties;
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
 * Dynamic pipeline orchestrator coordinating the interceptor chain of responsibility.
 *
 * <p>Supports two routing modes:
 *
 * <ul>
 *   <li><strong>DETERMINISTIC</strong> — Database-driven ordering via {@link
 *       PipelineConfigProvider}. Admin controls the exact interceptor sequence via the UI.
 *   <li><strong>AGENTIC</strong> — LLM-driven runtime sequence generation based on payload intent
 *       analysis (future — falls back to deterministic until LLM wiring is complete).
 * </ul>
 *
 * <p>Enforces the security governance kill-switch ({@code orasaka.security.disable-ai=true}). When
 * active, any interceptor returning {@code isAiDependent() == true} triggers a hard {@link
 * SecurityException}.
 *
 * @since 1.0.0
 */
@Component
public class DynamicPipelineOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(DynamicPipelineOrchestrator.class);

  private final Map<String, PromptContextInterceptor> interceptorRegistry;
  private final List<PromptContextInterceptor> fallbackChain;
  private final CoreProperties properties;
  private final SecurityProperties securityProperties;
  private final PipelineConfigProvider configProvider;

  private final AtomicReference<List<PromptContextInterceptor>> cachedChain =
      new AtomicReference<>();

  /**
   * Constructs the dynamic pipeline orchestrator.
   *
   * @param interceptors Registered PromptContextInterceptor beans (auto-discovered via Spring Boot
   *     auto-configuration from interceptor submodules).
   * @param properties Core configuration properties.
   * @param securityProperties Security governance configuration.
   * @param configProvider Database-driven pipeline config provider.
   */
  public DynamicPipelineOrchestrator(
      List<PromptContextInterceptor> interceptors,
      CoreProperties properties,
      SecurityProperties securityProperties,
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
    this.securityProperties =
        securityProperties != null ? securityProperties : new SecurityProperties();
    this.configProvider = configProvider;

    logger.info(
        "Initialized DynamicPipelineOrchestrator with {} registered interceptors. "
            + "Security kill-switch: {}",
        this.interceptorRegistry.size(),
        this.securityProperties.disableAi() ? "ACTIVE" : "inactive");
  }

  /**
   * Executes the interceptor pipeline chain on the raw user input query.
   *
   * @param rawUserQuery The original raw prompt.
   * @param context The execution context.
   * @return The enriched PromptContext, or null if the pipeline is disabled.
   * @throws SecurityException if an AI-dependent interceptor is invoked while the governance
   *     kill-switch is active.
   */
  public PromptContext process(String rawUserQuery, Context context) {
    boolean enabled = properties.orchestration() != null && properties.orchestration().enabled();
    if (!enabled) {
      return null;
    }

    logger.debug("Executing dynamic pipeline orchestration chain...");
    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put("userId", context.userId());
    userMetadata.put("conversationId", context.conversationId());
    userMetadata.putAll(context.preferences());
    userMetadata.put("roles", context.authorities().stream().map(Authority::name).toList());

    RoutingMode mode = resolveRoutingMode();
    PromptContext initialContext =
        new PromptContext(rawUserQuery, userMetadata, Map.of(), rawUserQuery, null, mode);
    List<PromptContextInterceptor> chain = resolveChain(mode);

    return chain.stream()
        .reduce(
            initialContext,
            (ctx, interceptor) -> {
              enforceSecurityGate(interceptor);
              try {
                return interceptor.intercept(ctx);
              } catch (Exception e) {
                logger.error(
                    "Error executing interceptor '{}'", interceptor.getClass().getName(), e);
                return ctx;
              }
            },
            (c1, c2) -> c1);
  }

  /**
   * Enforces the security governance kill-switch. Throws a hard {@link SecurityException} if the
   * interceptor is AI-dependent and the kill-switch is active.
   *
   * @param interceptor The interceptor to validate.
   * @throws SecurityException if the interceptor requires AI and the kill-switch is enabled.
   */
  private void enforceSecurityGate(PromptContextInterceptor interceptor) {
    if (securityProperties.disableAi() && interceptor.isAiDependent()) {
      String name = interceptor.getClass().getSimpleName();
      logger.error(
          "SECURITY GATE VIOLATION — AI-dependent interceptor '{}' blocked by governance "
              + "kill-switch (orasaka.security.disable-ai=true).",
          name);
      throw new SecurityException(
          "AI governance kill-switch is active. AI-dependent interceptor '"
              + name
              + "' is prohibited. Set orasaka.security.disable-ai=false to re-enable.");
    }
  }

  /**
   * Resolves the active routing mode from configuration.
   *
   * @return The configured routing mode, defaulting to DETERMINISTIC.
   */
  private RoutingMode resolveRoutingMode() {
    if (properties.orchestration() != null && properties.orchestration().routing() != null) {
      return properties.orchestration().routing().mode();
    }
    return RoutingMode.DETERMINISTIC;
  }

  /**
   * Resolves the active interceptor chain based on routing mode.
   *
   * @param mode The routing mode to use.
   * @return Ordered list of interceptors.
   */
  private List<PromptContextInterceptor> resolveChain(RoutingMode mode) {
    if (mode == RoutingMode.AGENTIC) {
      return resolveAgenticChain();
    }
    return resolveDeterministicChain();
  }

  /**
   * Resolves the deterministic chain from database configuration, with fallback to hardcoded
   * ordering.
   */
  private List<PromptContextInterceptor> resolveDeterministicChain() {
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
   * Resolves the agentic chain. Current implementation falls back to deterministic until LLM-driven
   * sequence generation is wired via tracked ADR.
   */
  private List<PromptContextInterceptor> resolveAgenticChain() {
    logger.debug(
        "AGENTIC routing mode active — falling back to deterministic until LLM wiring is complete.");
    return resolveDeterministicChain();
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
