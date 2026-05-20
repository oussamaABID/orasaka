package com.orasaka.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of OrasakaOptions.
 */
public record DefaultOrasakaOptions(
    Double temperature,
    Integer maxTokens,
    Map<String, Object> extraOptions
) implements OrasakaOptions {
    
    /**
     * Default constructor for an empty options object.
     */
    public DefaultOrasakaOptions() {
        this(null, null, new HashMap<>());
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    @Override
    public Integer getMaxTokens() {
        return maxTokens;
    }

    @Override
    public Map<String, Object> getExtraOptions() {
        return extraOptions;
    }

    @Override
    public OrasakaOptions withOption(String key, Object value) {
        Map<String, Object> newOptions = new HashMap<>(extraOptions);
        newOptions.put(key, value);
        return new DefaultOrasakaOptions(temperature, maxTokens, newOptions);
    }

    /**
     * Creates a new builder for {@link DefaultOrasakaOptions}.
     * 
     * @return A default instance of {@link DefaultOrasakaOptions}.
     */
    public static DefaultOrasakaOptions builder() {
        return new DefaultOrasakaOptions();
    }
}
