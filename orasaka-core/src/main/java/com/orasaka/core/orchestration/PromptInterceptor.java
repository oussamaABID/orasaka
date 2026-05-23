package com.orasaka.core.orchestration;

/** Standard interceptor interface for the prompt matrix pipeline. */
public interface PromptInterceptor {

  /**
   * Intercepts and enriches the PromptContext.
   *
   * @param context The current prompt context state.
   */
  void intercept(PromptContext context);

  /**
   * Declares the precedence execution order for the interceptor (lower value executes first).
   *
   * @return Execution order index.
   */
  int getOrder();
}
