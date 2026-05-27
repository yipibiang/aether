package com.helixent.coding.tools;

import com.helixent.foundation.tools.StructuredToolResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BashToolTest {

    @Test
    void returnsErrorForNullCommand() throws Exception {
        var tool = new BashTool();
        var params = new HashMap<String, Object>();
        params.put("command", null);
        var result = tool.invoke(params, null).get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void returnsErrorForBlankCommand() throws Exception {
        var tool = new BashTool();
        var result = tool.invoke(Map.of("command", "   "), null)
            .get(5, TimeUnit.SECONDS);
        assertInstanceOf(StructuredToolResult.Error.class, result);
    }

    @Test
    void executesEchoCommand() throws Exception {
        var tool = new BashTool();
        var result = tool.invoke(Map.of("command", "echo hello"), null)
            .get(10, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Success.class, result);
        @SuppressWarnings("unchecked")
        var success = (StructuredToolResult.Success<Map<String, Object>>) result;
        assertTrue(success.data().get("output").toString().contains("hello"));
    }

    @Test
    void returnsErrorForFailedCommand() throws Exception {
        var tool = new BashTool();
        var result = tool.invoke(Map.of("command", "exit 1"), null)
            .get(10, TimeUnit.SECONDS);

        assertInstanceOf(StructuredToolResult.Error.class, result);
        assertEquals("NONZERO_EXIT", ((StructuredToolResult.Error) result).code());
    }
}