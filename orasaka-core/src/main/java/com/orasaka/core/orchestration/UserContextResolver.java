package com.orasaka.core.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1: User Context Resolver Interceptor.
 *
 * <p>Pulls profile attributes and claims dynamically from the Security Context and appends them to
 * the prompt context metadata.
 */
@Component
public class UserContextResolver implements PromptInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(UserContextResolver.class);

  @Override
  public void intercept(PromptContext context) {
    logger.debug("Resolving user security context details...");
    var securityData = SecurityContextUtil.extractSecurityMetadata();
    if (!securityData.isEmpty()) {
      context.userMetadata().putAll(securityData);
      logger.debug(
          "Successfully enriched user metadata with security claims: {}", securityData.keySet());
    }
  }

  @Override
  public int getOrder() {
    return 1;
  }
}
