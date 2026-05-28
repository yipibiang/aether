package com.aether.tools;

import com.aether.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MkdirToolTest {

    @Test
    void createsDirectory(@TempDir Path tempDir) throws Exception {
        var dirPath = tempDir.resolve("newdir");

        var tool = new MkdirTool();
        var result = tool.invoke(Map.of("path", dirPath.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        assertTrue(Files.isDirectory(dirPath));
    }

    @Test
    void createsNestedDirectories(@TempDir Path tempDir) throws Exception {
        var dirPath = tempDir.resolve("a/b/c");

        var tool = new MkdirTool();
        tool.invoke(Map.of("path", dirPath.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertTrue(Files.isDirectory(dirPath));
    }

    @Test
    void returnsErrorForExistingDirectory(@TempDir Path tempDir) throws Exception {
        var dirPath = tempDir.resolve("existing");
        Files.createDirectory(dirPath);

        var tool = new MkdirTool();
        var result = tool.invoke(Map.of("path", dirPath.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("ALREADY_EXISTS", ((StructuredToolResult.Error) result).code());
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new MkdirTool();
        var params = new HashMap<String, Object>();
        params.put("path", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}