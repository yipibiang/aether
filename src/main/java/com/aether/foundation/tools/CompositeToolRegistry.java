package com.aether.foundation.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Merges multiple {@link ToolRegistry} instances; first registration wins on duplicate names. */
public final class CompositeToolRegistry implements ToolRegistry {

    private final List<ToolRegistry> registries;

    public CompositeToolRegistry(List<ToolRegistry> registries) {
        this.registries = registries != null ? List.copyOf(registries) : List.of();
    }

    public CompositeToolRegistry(ToolRegistry... registries) {
        this(List.of(registries));
    }

    @Override
    public List<Tool> tools() {
        var byName = new LinkedHashMap<String, Tool>();
        for (var registry : registries) {
            for (var tool : registry.tools()) {
                byName.putIfAbsent(tool.name(), tool);
            }
        }
        return List.copyOf(byName.values());
    }

    public static ToolRegistry of(ToolRegistry first, ToolRegistry second) {
        return new CompositeToolRegistry(List.of(first, second));
    }

    public static ToolRegistry of(List<Tool> tools) {
        return () -> List.copyOf(tools);
    }
}
