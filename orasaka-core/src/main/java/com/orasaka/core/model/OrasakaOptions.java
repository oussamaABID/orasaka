package com.orasaka.core.model;

import java.util.Map;

/**
 * Unified options interface for Orasaka models.
 * Maps internal Spring AI options to a common interface.
 */
public interface OrasakaOptions {
    
    /**
     * Get the temperature for the model.
     * @return Temperature value or null if not applicable.
     */
    Double getTemperature();

    /**
     * Get the maximum number of tokens to generate.
     * @return Max tokens or null if not applicable.
     */
    Integer getMaxTokens();

    /**
     * Get provider-specific metadata or extra options.
     * @return Map of extra options.
     */
    Map<String, Object> getExtraOptions();

    /**
     * Builder-like method to set a specific provider option.
     * 
     * @param key The option key.
     * @param value The option value.
     * @return A new instance with the updated option.
     */
    OrasakaOptions withOption(String key, Object value);
}
