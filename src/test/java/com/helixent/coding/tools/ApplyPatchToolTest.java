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

class ApplyPatchToolTest {

    @Test
    void appliesSimplePatch(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "line1\nline2\nline3\n");

        var patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,3 +1,3 @@
             line1
            -line2
            +modified
             line3
            """;

        var tool = new ApplyPatchTool();
        var result = tool.invoke(Map.of("file_path", file.toString(), "patch", patch), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);

        var content = Files.readString(file);
        assertTrue(content.contains("modified"));
        assertFalse(content.contains("line2"));
    }

    @Test
    void returnsErrorForNullPath() throws Exception {
        var tool = new ApplyPatchTool();
        var params = new HashMap<String, Object>();
        params.put("file_path", null);
        params.put("patch", "patch");
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForNullPatch() throws Exception {
        var tool = new ApplyPatchTool();
        var params = new HashMap<String, Object>();
        params.put("file_path", "/tmp/test.txt");
        params.put("patch", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForInvalidPatch() throws Exception {
        var tool = new ApplyPatchTool();
        var result = tool.invoke(Map.of("file_path", "/tmp/test.txt", "patch", "not a valid patch"), null)
            .get(5, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("INVALID_PATCH", ((StructuredToolResult.Error) result).code());
    }
}