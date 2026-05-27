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

class ListFilesToolTest {

    @Test
    void listsFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "a");
        Files.writeString(tempDir.resolve("b.txt"), "b");

        var tool = new ListFilesTool();
        var result = tool.invoke(Map.of("path", tempDir.toString()), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertTrue(((String) success.data().get("content")).contains("a.txt"));
        assertTrue(((String) success.data().get("content")).contains("b.txt"));
    }

    @Test
    void returnsErrorForNonExistentDirectory() throws Exception {
        var tool = new ListFilesTool();
        var result = tool.invoke(Map.of("path", "/nonexistent/dir"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new ListFilesTool();
        var params = new HashMap<String, Object>();
        params.put("path", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }
}