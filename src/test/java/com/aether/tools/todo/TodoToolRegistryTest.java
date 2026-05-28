package com.aether.tools.todo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TodoToolRegistryTest {

    @Test
    void tools_exposesTodoWrite() {
        var names = new TodoToolRegistry().tools().stream().map(t -> t.name()).toList();
        assertEquals(List.of(TodoWriteTool.NAME), names);
    }

    @Test
    void middleware_isPairedWithRegistry() {
        assertNotNull(new TodoToolRegistry().middleware());
    }
}
