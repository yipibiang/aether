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

class WriteFileToolTest {

    @Test
    void writesFile(@TempDir Path tempDir) throws Exception {
        var tool = new WriteFileTool();
        var filePath = tempDir.resolve("test.txt").toString();

        var result = tool.invoke(Map.of("file_path", filePath, "content", "hello\nworld"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        assertTrue(Files.exists(Path.of(filePath)));
        assertEquals("hello\nworld", Files.readString(Path.of(filePath)));
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new WriteFileTool();
        var params = new HashMap<String, Object>();
        params.put("file_path", null);
        params.put("content", "test");
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullContent() throws Exception {
        var tool = new WriteFileTool();
        var params = new HashMap<String, Object>();
        params.put("file_path", "/tmp/test.txt");
        params.put("content", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void createsParentDirectories(@TempDir Path tempDir) throws Exception {
        var tool = new WriteFileTool();
        var filePath = tempDir.resolve("sub/dir/test.txt").toString();

        tool.invoke(Map.of("file_path", filePath, "content", "data"), null)
            .get(5, TimeUnit.SECONDS);

        assertTrue(Files.exists(Path.of(filePath)));
    }
}