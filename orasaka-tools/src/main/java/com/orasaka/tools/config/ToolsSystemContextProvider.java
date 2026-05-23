package com.orasaka.tools.config;

import com.orasaka.core.pipeline.SystemContextProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Downstream system context provider implementation for tools execution matrix data.
 *
 * <p>Registered automatically inside the host application using IoC.
 */
@Component
public class ToolsSystemContextProvider implements SystemContextProvider {

  @Override
  public Map<String, Object> getSystemContext() {
    return Map.of(
        "activeTools", "searchWeb, ttsGenerator, imageGenerator",
        "systemStatus", "OPERATIONAL",
        "activeTrends", "AI-agentic-flows, virtual-threads-concurrency");
  }
}
