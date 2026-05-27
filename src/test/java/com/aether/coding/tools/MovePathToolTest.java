package com.aether.coding.tools;

import com.aether.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MovePathToolTest {

    @Test
    void movesFile(@TempDir Path tempDir) throws Exception {
        var src = tempDir.resolve("src.txt");
        var dst = tempDir.resolve("dst.txt");
        Files.writeString(src, "content");

        var tool = new MovePathTool();
        var result = tool.invoke(Map.of("source", src.toString(), "destination", dst.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        assertFalse(Files.exists(src));
        assertTrue(Files.exists(dst));
    }

    @Test
    void returnsErrorForNonExistentSource() throws Exception {
        var tool = new MovePathTool();
        var result = tool.invoke(Map.of("source", "/nonexistent/src", "destination", "/tmp/dst"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("NOT_FOUND", ((StructuredToolResult.Error) result).code());
    }

    @Test
    void returnsErrorForNullSource() throws Exception {
        var tool = new MovePathTool();
        var params = new HashMap<String, Object>();
        params.put("source", null);
        params.put("destination", "/tmp/dst");
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullDestination() throws Exception {
        var tool = new MovePathTool();
        var params = new HashMap<String, Object>();
        params.put("source", "/tmp/src");
        params.put("destination", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}