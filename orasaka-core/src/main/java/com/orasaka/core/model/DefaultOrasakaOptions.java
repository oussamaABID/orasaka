package com.orasaka.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Default record implementation of the {@link OrasakaOptions} configuration interface.
 *
 * <p>Wraps execution parameters (such as temperature and token limits) and supports generic extra
 * options dynamic mappings for provider-specific customizations.
 *
 * @param temperature The sampling temperature to control the creativity of LLM responses. Must be a
 *     value between 0.0 and 2.0.
 * @param maxTokens The maximum number of tokens to generate in the LLM response.
 * @param extraOptions A map of additional configuration options passed dynamically to downstream
 *     providers.
 * @see com.orasaka.core.model.OrasakaOptions
 * @see com.orasaka.core.model.OrasakaChatRequest
 */
public record DefaultOrasakaOptions(
    Double temperature, Integer maxTokens, Map<String, Object> extraOptions)
    implements OrasakaOptions {

  /**
   * Default constructor creating an empty configuration with null constraints and an empty extra
   * options map.
   */
  public DefaultOrasakaOptions() {
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
   * <p>Creates a new instance of {@link DefaultOrasakaOptions} defending the original values.
   *
   * @param key The option key identifier.
   * @param value The value associated with the key parameter.
   * @return A new instance of {@link OrasakaOptions} containing the updated settings.
   */
  @Override
  public OrasakaOptions withOption(String key, Object value) {
    Map<String, Object> newOptions = new HashMap<>(extraOptions);
    newOptions.put(key, value);
    return new DefaultOrasakaOptions(temperature, maxTokens, newOptions);
  }

  /**
   * Factory method creating a new builder/empty instance of {@link DefaultOrasakaOptions}.
   *
   * @return A default empty instance of {@link DefaultOrasakaOptions}.
   */
  public static DefaultOrasakaOptions builder() {
    return new DefaultOrasakaOptions();
  }
}
