package com.helixent.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultPolicyTest {

    @Test
    void forTool_returnsSummaryFirstPolicyForSearchAndInspectionTools() {
        var policy = ToolResultPolicy.forTool("list_files");
        assertTrue(policy.preferSummaryOnly());
        assertFalse(policy.includeData());
        assertEquals(1000, policy.maxStringLength());
    }

    @Test
    void forTool_returnsSummaryFirstPolicyForGlobSearch() {
        var policy = ToolResultPolicy.forTool("glob_search");
        assertTrue(policy.preferSummaryOnly());
        assertFalse(policy.includeData());
    }

    @Test
    void forTool_returnsSummaryFirstPolicyForGrepSearch() {
        var policy = ToolResultPolicy.forTool("grep_search");
        assertTrue(policy.preferSummaryOnly());
        assertFalse(policy.includeData());
    }

    @Test
    void forTool_returnsDataCarryingPolicyForReadFile() {
        var policy = ToolResultPolicy.forTool("read_file");
        assertFalse(policy.preferSummaryOnly());
        assertTrue(policy.includeData());
        assertEquals(12000, policy.maxStringLength());
    }

    @Test
    void forTool_returnsDefaultPolicyForUnknownTools() {
        var policy = ToolResultPolicy.forTool("unknown_tool");
        assertFalse(policy.preferSummaryOnly());
        assertTrue(policy.includeData());
        assertEquals(4000, policy.maxStringLength());
    }

    @Test
    void forTool_returnsDataCarryingPolicyForWriteTools() {
        for (var tool : new String[]{"apply_patch", "write_file", "str_replace"}) {
            var policy = ToolResultPolicy.forTool(tool);
            assertFalse(policy.preferSummaryOnly());
            assertTrue(policy.includeData());
            assertEquals(4000, policy.maxStringLength());
        }
    }
}