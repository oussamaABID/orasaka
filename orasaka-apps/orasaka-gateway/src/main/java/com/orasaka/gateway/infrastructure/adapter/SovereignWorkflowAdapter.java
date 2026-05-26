package com.orasaka.gateway.infrastructure.adapter;

import com.orasaka.business.domain.model.SovereignWorkflowContext;
import com.orasaka.business.domain.port.SovereignWorkflowOrchestrator;
import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.ChatRequest;
import com.orasaka.core.domain.model.chat.ChatResponse;
import com.orasaka.core.domain.ports.inbound.AiClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Gateway adapter translating the Business layer's {@link SovereignWorkflowContext} into the Core
 * infrastructure's {@link Context} and {@link ChatRequest}.
 *
 * <p>This is the <b>sole translation boundary</b> between the Business hexagon (which owns "What")
 * and the Core engine (which owns "How"). All business context fields are mapped into the Core's
 * {@code Context.preferences} map using namespaced keys:
 *
 * <ul>
 *   <li>{@code orasaka.pipeline.contextId} — workflow execution identifier
 *   <li>{@code orasaka.pipeline.forcedInterceptors} — interceptor keys that must execute
 *   <li>{@code orasaka.pipeline.skippedInterceptors} — interceptor keys to bypass
 *   <li>{@code orasaka.user.tier} — subscription/RBAC tier
 *   <li>{@code orasaka.user.meta.*} — arbitrary business metadata entries
 * </ul>
 *
 * @see SovereignWorkflowOrchestrator
 * @see SovereignWorkflowContext
 * @since 1.0.0
 */
@Component
public class SovereignWorkflowAdapter implements SovereignWorkflowOrchestrator {

  private final AiClient aiClient;

  public SovereignWorkflowAdapter(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  @Override
  public String executeSovereignPrompt(
      String userPrompt, SovereignWorkflowContext workflowContext) {
    List<ChatRequest.ChatMessage> messages =
        List.of(new ChatRequest.ChatMessage("system", workflowContext.systemInstructions()));

    Map<String, Object> preferences = mapContextToPreferences(workflowContext);
    Context context = new Context("anonymous", workflowContext.contextId(), preferences, Set.of());
    ChatRequest infrastructureRequest = new ChatRequest(userPrompt, messages, Map.of(), context);

    ChatResponse response = aiClient.chat(infrastructureRequest);

    return response != null ? response.content() : "";
  }

  /**
   * Maps {@link SovereignWorkflowContext} fields into the Core's {@code Context.preferences} map
   * using the {@code orasaka.pipeline.*} and {@code orasaka.user.*} namespaces.
   */
  private static Map<String, Object> mapContextToPreferences(
      SovereignWorkflowContext workflowContext) {
    Map<String, Object> preferences = new HashMap<>();

    // Pipeline-level directives
    preferences.put("orasaka.pipeline.contextId", workflowContext.contextId());
    preferences.put("orasaka.pipeline.forcedInterceptors", workflowContext.forcedInterceptors());
    preferences.put("orasaka.pipeline.skippedInterceptors", workflowContext.skippedInterceptors());

    // User-level attributes
    preferences.put("orasaka.user.tier", workflowContext.userTier());

    // Flatten business metadata into the orasaka.user.meta namespace
    workflowContext
        .metadata()
        .forEach((key, value) -> preferences.put("orasaka.user.meta." + key, value));

    return Map.copyOf(preferences);
  }
}
