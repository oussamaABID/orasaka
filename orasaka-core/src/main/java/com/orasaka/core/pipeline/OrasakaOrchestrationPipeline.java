package com.orasaka.core.pipeline;

import com.orasaka.core.engine.CoreProperties;
import com.orasaka.core.support.OrasakaContext;
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
      if (context.roles() != null) {
        userMetadata.put("roles", context.roles());
      }
    }

    PromptContext initialContext = new PromptContext(rawUserQuery, userMetadata);
    return interceptors.stream()
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
}
