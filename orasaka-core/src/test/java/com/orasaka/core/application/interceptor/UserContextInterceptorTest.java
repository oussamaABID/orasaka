package com.orasaka.core.application.interceptor;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

class UserContextInterceptorTest {

  @Test
  @DisplayName("preProcess does nothing when userContext is disabled")
  void disabledUserContext() {
    var userContext = new CoreProperties.UserContextConfig(false);
    var orchestration = new CoreProperties.OrchestrationConfig(true, userContext, null, null, null);
    var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);

    var interceptor = new UserContextInterceptor(props);
    var request =
        new InternalChatRequest(
            "hello",
            List.of(),
            null,
            new Context(
                "u1",
                "conv-test",
                Map.of(
                    "primary_industry", "healthcare",
                    "ai_behavior", "Be pedagogical and verbose"),
                null));
    List<Message> messages = new ArrayList<>();
    ChatOptions options = Mockito.mock(ChatOptions.class);

    var resultOptions = interceptor.preProcess(request, "hello", messages, options);
    assertSame(options, resultOptions);
    assertTrue(messages.isEmpty());
  }

  @Test
  @DisplayName(
      "preProcess injects system prompt constraints when enabled and user profile exists in context preferences")
  void enabledUserContextInjectsConstraints() {
    var userContext = new CoreProperties.UserContextConfig(true);
    var orchestration = new CoreProperties.OrchestrationConfig(true, userContext, null, null, null);
    var props = new CoreProperties("ollama", null, null, orchestration, null, null, null, null);

    var interceptor = new UserContextInterceptor(props);
    var request =
        new InternalChatRequest(
            "hello",
            List.of(),
            null,
            new Context(
                "u1",
                "conv-test",
                Map.of(
                    "primary_industry", "healthcare",
                    "ai_behavior", "Be pedagogical and verbose"),
                null));
    List<Message> messages = new ArrayList<>();
    ChatOptions options = Mockito.mock(ChatOptions.class);

    var resultOptions = interceptor.preProcess(request, "hello", messages, options);
    assertSame(options, resultOptions);
    assertEquals(1, messages.size());
    String text = messages.get(0).getText();
    assertTrue(text.contains("System constraints:"));
    assertTrue(text.contains("User Primary Industry: healthcare"));
    assertTrue(text.contains("User AI Behavior: Be pedagogical and verbose"));
  }
}
