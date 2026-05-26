package com.orasaka.core.application.interceptor;

import com.orasaka.core.domain.model.PromptContext;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Order 2 — Single atomic service responsible for both resolving system/environment state and
 * injecting it into the timeline.
 *
 */
@Component
class SystemContextInjector implements PromptContextInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(SystemContextInjector.class);

  public SystemContextInjector() {
    // Zero-dependency atomic initialization
  }

  @Override
  public PromptContext intercept(PromptContext context) {
    logger.debug("Injecting system context signals directly.");
    var newSystemMetadata = new HashMap<>(context.systemMetadata());

    // Resolve system/environment state directly (previously resolved via providers)
    Map<String, Object> systemData =
        Map.of(
            "activeTools", "searchWeb, ttsGenerator, imageGenerator",
            "systemStatus", "OPERATIONAL",
            "activeTrends", "AI-agentic-flows, virtual-threads-concurrency");

    newSystemMetadata.putAll(systemData);

    return context.withSystemMetadata(Map.copyOf(newSystemMetadata));
  }

  @Override
  public int getOrder() {
    return 2;
  }
}
