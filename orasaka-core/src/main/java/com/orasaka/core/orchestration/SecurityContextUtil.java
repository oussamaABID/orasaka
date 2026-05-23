package com.orasaka.core.orchestration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflective security context utility to extract authentication details dynamically.
 *
 * <p>Avoids tight coupling with Spring Security core at compile-time to maintain clean
 * architectural decoupling (No Starter Leaks).
 */
public final class SecurityContextUtil {

  private SecurityContextUtil() {}

  /**
   * Reflectively pulls principal attributes from the active SecurityContextHolder thread-local
   * context.
   *
   * @return Map of credentials details or an empty map if unauthenticated.
   */
  public static Map<String, Object> extractSecurityMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    try {
      Class<?> holderClass =
          Class.forName("org.springframework.security.core.context.SecurityContextHolder");
      Object context = holderClass.getMethod("getContext").invoke(null);
      Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
      if (auth != null) {
        Object principal = auth.getClass().getMethod("getPrincipal").invoke(auth);
        if (principal != null) {
          metadata.put("principalClass", principal.getClass().getName());
          safeInvoke(principal, "id", metadata, "userId");
          safeInvoke(principal, "username", metadata, "username");
          safeInvoke(principal, "email", metadata, "email");
          safeInvoke(principal, "rateLimitTier", metadata, "rateLimitTier");

          // Reflectively extract user preferences map if available
          try {
            Method prefMethod = principal.getClass().getMethod("preferences");
            Object prefs = prefMethod.invoke(principal);
            if (prefs instanceof Map<?, ?> map) {
              for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                  metadata.put("preference." + entry.getKey(), entry.getValue());
                }
              }
            }
          } catch (Exception ignored) {
          }
        }
      }
    } catch (Exception ignored) {
      // Spring Security is either absent or the current context has no active authentication
    }
    return metadata;
  }

  private static void safeInvoke(
      Object obj, String methodName, Map<String, Object> target, String key) {
    try {
      Method method = obj.getClass().getMethod(methodName);
      Object val = method.invoke(obj);
      if (val != null) {
        target.put(key, val.toString());
      }
    } catch (Exception ignored) {
    }
  }
}
