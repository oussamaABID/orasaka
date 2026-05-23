package com.orasaka.core.orchestration;

import com.orasaka.core.config.CoreProperties;
import com.orasaka.core.context.OrasakaContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Coordination manager for the prompt interceptor pipeline chain of responsibility. */
@Component
public class OrasakaOrchestrationPipeline {

  private static final Logger logger = LoggerFactory.getLogger(OrasakaOrchestrationPipeline.class);

  private final List<PromptInterceptor> interceptors;
  private final CoreProperties properties;

  /**
   * Constructs the orchestration pipeline.
   *
   * @param interceptors Registered PromptInterceptor list.
   * @param properties Configuration properties.
   */
  public OrasakaOrchestrationPipeline(
      List<PromptInterceptor> interceptors, CoreProperties properties) {
    List<PromptInterceptor> sorted =
        new ArrayList<>(interceptors != null ? interceptors : List.of());
    sorted.sort(Comparator.comparingInt(PromptInterceptor::getOrder));
    this.interceptors = List.copyOf(sorted);
    this.properties = properties;
    logger.info(
        "Initialized Prompt Orchestration Pipeline with {} interceptors.",
        this.interceptors.size());
  }

  /**
   * Executes the interceptor pipeline chain on the raw user input query.
   *
   * @param rawUserQuery The original raw prompt.
   * @param context The execution context.
   * @return The enriched PromptContext, or null if the pipeline is disabled.
   */
  public PromptContext process(String rawUserQuery, OrasakaContext context) {
    boolean enabled = properties.orchestration() != null && properties.orchestration().enabled();
    if (!enabled) {
      // Strict rule: Zero runtime memory allocation when disabled (bypass completely)
      return null;
    }

    logger.debug("Executing prompt matrix orchestration chain...");
    Map<String, Object> userMetadata = new HashMap<>();
    if (context != null) {
      userMetadata.put("userId", context.userId());
      userMetadata.put("conversationId", context.conversationId());
      if (context.preferences() != null) {
        userMetadata.putAll(context.preferences());
      }
      if (context.authorities() != null) {
        userMetadata.put("authorities", context.authorities());
      }
    }

    PromptContext promptContext = new PromptContext(rawUserQuery, userMetadata);
    for (PromptInterceptor interceptor : interceptors) {
      try {
        interceptor.intercept(promptContext);
      } catch (Exception e) {
        logger.error(
            "Error executing prompt interceptor '{}'", interceptor.getClass().getName(), e);
      }
    }
    return promptContext;
  }
}
