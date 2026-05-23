package com.orasaka.core.pipeline;

/** Standard interceptor interface for the prompt matrix pipeline. */
interface PromptInterceptor {

  /**
   * Intercepts and enriches the PromptContext.
   *
   * @param context The current prompt context state.
   * @return The updated prompt context state.
   */
  PromptContext intercept(PromptContext context);

  /**
   * Declares the precedence execution order for the interceptor (lower value executes first).
   *
   * @return Execution order index.
   */
  int getOrder();
}
