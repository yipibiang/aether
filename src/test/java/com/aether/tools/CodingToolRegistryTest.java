package com.aether.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodingToolRegistryTest {

    @Test
    void tools_exposesCoreCodingToolNames() {
        var names = new CodingToolRegistry().tools().stream()
            .map(t -> t.name())
            .sorted()
            .toList();
        assertTrue(names.contains("bash"));
        assertTrue(names.contains("read_file"));
        assertTrue(names.contains("apply_patch"));
        assertEquals(11, names.size());
    }
}
