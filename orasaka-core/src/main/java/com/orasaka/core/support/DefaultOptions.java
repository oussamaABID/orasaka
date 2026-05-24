package com.orasaka.core.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default record implementation of the {@link Options} configuration interface.
 *
 * <p>Wraps execution parameters (such as temperature and token limits) and supports generic extra
 * options dynamic mappings for provider-specific customizations.
 *
 * @param temperature The sampling temperature to control the creativity of LLM responses. Must be a
 *     value between 0.0 and 2.0.
 * @param maxTokens The maximum number of tokens to generate in the LLM response.
 * @param extraOptions A map of additional configuration options passed dynamically to downstream
 *     providers.
 * @see com.orasaka.core.support.Options
 * @see com.orasaka.core.support.ChatRequest
 */
public record DefaultOptions(
    Double temperature, Integer maxTokens, Map<String, Object> extraOptions) implements Options {

  public DefaultOptions {
    extraOptions =
        Optional.ofNullable(extraOptions)
            .map(
                m -> {
                  var clean = new HashMap<String, Object>();
                  m.forEach(
                      (k, v) -> {
                        if (k != null && v != null) {
                          clean.put(k, v);
                        }
                      });
                  return Map.copyOf(clean);
                })
            .orElseGet(Map::of);
  }

  /**
   * Default constructor creating an empty configuration with null constraints and an empty extra
   * options map.
   */
  public DefaultOptions() {
    this(null, null, new HashMap<>());
  }

  /**
   * Retrieves the configured LLM sampling temperature.
   *
   * @return The temperature value, or {@code null} if not set.
   */
  @Override
  public Double getTemperature() {
    return temperature;
  }

  /**
   * Retrieves the maximum tokens limit configured for response generation.
   *
   * @return The maximum tokens count, or {@code null} if not set.
   */
  @Override
  public Integer getMaxTokens() {
    return maxTokens;
  }

  /**
   * Retrieves the map of provider-specific extra options.
   *
   * @return An immutable or mutable map representing custom key-value settings.
   */
  @Override
  public Map<String, Object> getExtraOptions() {
    return extraOptions;
  }

  /**
   * Immutable builder pattern method to associate a custom option key-value pair.
   *
   * <p>Creates a new instance of {@link DefaultOptions} defending the original values.
   *
   * @param key The option key identifier.
   * @param value The value associated with the key parameter.
   * @return A new instance of {@link Options} containing the updated settings.
   */
  @Override
  public Options withOption(String key, Object value) {
    Map<String, Object> newOptions = new HashMap<>(extraOptions);
    newOptions.put(key, value);
    return new DefaultOptions(temperature, maxTokens, newOptions);
  }

  /**
   * Factory method creating a new builder/empty instance of {@link DefaultOptions}.
   *
   * @return A default empty instance of {@link DefaultOptions}.
   */
  public static DefaultOptions builder() {
    return new DefaultOptions();
  }
}
