package com.orasaka.core.pipeline;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflective security context utility to extract authentication details dynamically.
 *
 * <p>Avoids tight coupling with Spring Security core at compile-time to maintain clean
 * architectural decoupling (No Starter Leaks).
 */
final class SecurityContextUtil {

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

          try {
            Method prefMethod = principal.getClass().getMethod("preferences");
            Object prefs = prefMethod.invoke(principal);
            if (prefs instanceof Map<?, ?> map) {
              map.entrySet().stream()
                  .filter(entry -> entry.getKey() != null)
                  .forEach(entry -> metadata.put("preference." + entry.getKey(), entry.getValue()));
            }
          } catch (Exception ignored) {
          }
        }
      }
    } catch (Exception ignored) {
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
