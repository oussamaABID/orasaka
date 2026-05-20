package com.orasaka.tools.functions;

import com.orasaka.core.tool.OrasakaToolRegistry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Default implementation of OrasakaToolRegistry.
 * Registry for local Java methods to be used as tools by the AI models.
 */
@Component
public class DefaultOrasakaToolRegistry implements OrasakaToolRegistry {

    private final List<ToolCallback> registeredTools = new ArrayList<>();

    @Override
    public <I, O> void registerTool(String name, String description, Class<I> inputType, Function<I, O> function) {
        this.registeredTools.add(FunctionToolCallback.builder(name, function)
                .description(description)
                .inputType(inputType)
                .build());
    }

    @Override
    public List<ToolCallback> getRegisteredTools() {
        return List.copyOf(registeredTools);
    }
}
