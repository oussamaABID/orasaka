package com.orasaka.business.domain.port;

public interface SovereignWorkflowOrchestrator {
  String executeSovereignPrompt(String userPrompt, String systemInstructions, String contextId);
}
