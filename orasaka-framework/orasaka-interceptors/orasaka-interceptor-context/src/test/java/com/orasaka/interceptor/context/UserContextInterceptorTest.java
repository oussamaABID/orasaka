package com.orasaka.interceptor.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orasaka.core.domain.model.Context;
import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;

class UserContextInterceptorTest {

  @Test
  @DisplayName("preProcess does nothing when user context orchestration is disabled")
  void disabledUserContext() {
    CoreProperties props = mock(CoreProperties.class);
    when(props.orchestration()).thenReturn(null);

    UserContextInterceptor interceptor = new UserContextInterceptor(props);
    InternalChatRequest request = mock(InternalChatRequest.class);
    List<Message> messages = new ArrayList<>();
    ChatOptions options = mock(ChatOptions.class);

    ChatOptions result = interceptor.preProcess(request, "prompt", messages, options);
    assertThat(result).isSameAs(options);
    assertThat(messages).isEmpty();
  }

  @Test
  @DisplayName("preProcess injects constraints from preferences (snake_case)")
  void injectsConstraintsSnakeCase() {
    CoreProperties props = mock(CoreProperties.class);
    CoreProperties.OrchestrationConfig orchestration =
        mock(CoreProperties.OrchestrationConfig.class);
    CoreProperties.UserContextConfig userCtx = mock(CoreProperties.UserContextConfig.class);
    when(props.orchestration()).thenReturn(orchestration);
    when(orchestration.userContext()).thenReturn(userCtx);
    when(userCtx.enabled()).thenReturn(true);

    UserContextInterceptor interceptor = new UserContextInterceptor(props);

    InternalChatRequest request = mock(InternalChatRequest.class);
    Context context = mock(Context.class);
    Map<String, Object> preferences =
        Map.of(
            "primary_industry", "Finance",
            "ai_behavior", "Helpful assistant");
    when(request.context()).thenReturn(context);
    when(context.preferences()).thenReturn(preferences);

    List<Message> messages = new ArrayList<>();
    ChatOptions options = mock(ChatOptions.class);

    interceptor.preProcess(request, "prompt", messages, options);

    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getText())
        .contains("System constraints:")
        .contains("User Primary Industry: Finance")
        .contains("User AI Behavior: Helpful assistant");
  }

  @Test
  @DisplayName("preProcess injects constraints from preferences (camelCase)")
  void injectsConstraintsCamelCase() {
    CoreProperties props = mock(CoreProperties.class);
    CoreProperties.OrchestrationConfig orchestration =
        mock(CoreProperties.OrchestrationConfig.class);
    CoreProperties.UserContextConfig userCtx = mock(CoreProperties.UserContextConfig.class);
    when(props.orchestration()).thenReturn(orchestration);
    when(orchestration.userContext()).thenReturn(userCtx);
    when(userCtx.enabled()).thenReturn(true);

    UserContextInterceptor interceptor = new UserContextInterceptor(props);

    InternalChatRequest request = mock(InternalChatRequest.class);
    Context context = mock(Context.class);
    Map<String, Object> preferences =
        Map.of(
            "primaryIndustry", "Healthcare",
            "aiBehavior", "Professional advisor");
    when(request.context()).thenReturn(context);
    when(context.preferences()).thenReturn(preferences);

    List<Message> messages = new ArrayList<>();
    ChatOptions options = mock(ChatOptions.class);

    interceptor.preProcess(request, "prompt", messages, options);

    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getText())
        .contains("System constraints:")
        .contains("User Primary Industry: Healthcare")
        .contains("User AI Behavior: Professional advisor");
  }

  @Test
  @DisplayName("preProcess handles null or empty preference values gracefully")
  void emptyPreferencesGraceful() {
    CoreProperties props = mock(CoreProperties.class);
    CoreProperties.OrchestrationConfig orchestration =
        mock(CoreProperties.OrchestrationConfig.class);
    CoreProperties.UserContextConfig userCtx = mock(CoreProperties.UserContextConfig.class);
    when(props.orchestration()).thenReturn(orchestration);
    when(orchestration.userContext()).thenReturn(userCtx);
    when(userCtx.enabled()).thenReturn(true);

    UserContextInterceptor interceptor = new UserContextInterceptor(props);

    InternalChatRequest request = mock(InternalChatRequest.class);
    Context context = mock(Context.class);
    Map<String, Object> preferences = new HashMap<>();
    preferences.put("primary_industry", "");
    preferences.put("ai_behavior", null);
    when(request.context()).thenReturn(context);
    when(context.preferences()).thenReturn(preferences);

    List<Message> messages = new ArrayList<>();
    ChatOptions options = mock(ChatOptions.class);

    interceptor.preProcess(request, "prompt", messages, options);

    assertThat(messages).isEmpty();
  }

  @Test
  @DisplayName("preProcess handles exceptions gracefully without propagating")
  void exceptionSafety() {
    CoreProperties props = mock(CoreProperties.class);
    CoreProperties.OrchestrationConfig orchestration =
        mock(CoreProperties.OrchestrationConfig.class);
    CoreProperties.UserContextConfig userCtx = mock(CoreProperties.UserContextConfig.class);
    when(props.orchestration()).thenReturn(orchestration);
    when(orchestration.userContext()).thenReturn(userCtx);
    when(userCtx.enabled()).thenReturn(true);

    UserContextInterceptor interceptor = new UserContextInterceptor(props);

    InternalChatRequest request = mock(InternalChatRequest.class);
    // request.context() throwing exception to trigger catch block
    when(request.context()).thenThrow(new RuntimeException("Simulated Database / Context failure"));

    List<Message> messages = new ArrayList<>();
    ChatOptions options = mock(ChatOptions.class);

    assertThatCode(() -> interceptor.preProcess(request, "prompt", messages, options))
        .doesNotThrowAnyException();
    assertThat(messages).isEmpty();
  }
}
