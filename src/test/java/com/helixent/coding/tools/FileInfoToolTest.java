package com.helixent.coding.tools;

import com.helixent.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FileInfoToolTest {

    @Test
    void returnsFileInfo(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        var tool = new FileInfoTool();
        var result = tool.invoke(Map.of("path", file.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertEquals(true, success.data().get("isFile"));
        assertEquals("txt", success.data().get("extension"));
    }

    @Test
    void returnsDirectoryInfo(@TempDir Path tempDir) throws Exception {
        var tool = new FileInfoTool();
        var result = tool.invoke(Map.of("path", tempDir.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertEquals(true, success.data().get("isDirectory"));
    }

    @Test
    void returnsErrorForNonExistentPath() throws Exception {
        var tool = new FileInfoTool();
        var result = tool.invoke(Map.of("path", "/nonexistent/path"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("NOT_FOUND", ((StructuredToolResult.Error) result).code());
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new FileInfoTool();
        var params = new HashMap<String, Object>();
        params.put("path", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}