package com.helixent.coding.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ToolUtilsTest {

    @Test
    void ensureFilePath_returnsErrorForNullPath() {
        var result = ToolUtils.ensureFilePath(null);
        assertFalse(result.ok());
        assertEquals("Path is required", result.error());
    }

    @Test
    void ensureFilePath_returnsErrorForBlankPath() {
        var result = ToolUtils.ensureFilePath("   ");
        assertFalse(result.ok());
        assertEquals("Path is required", result.error());
    }

    @Test
    void ensureFilePath_returnsErrorForNonExistentFile() {
        var result = ToolUtils.ensureFilePath("/nonexistent/file.txt");
        assertFalse(result.ok());
        assertTrue(result.error().contains("File not found"));
    }

    @Test
    void ensureFilePath_returnsOkForExistingFile(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        var result = ToolUtils.ensureFilePath(file.toString());
        assertTrue(result.ok());
        assertNull(result.error());
    }

    @Test
    void ensureFilePath_returnsErrorForDirectory(@TempDir Path tempDir) {
        var result = ToolUtils.ensureFilePath(tempDir.toString());
        assertFalse(result.ok());
        assertTrue(result.error().contains("not a file"));
    }

    @Test
    void ensureDirectoryPath_returnsErrorForNullPath() {
        var result = ToolUtils.ensureDirectoryPath(null);
        assertFalse(result.ok());
        assertEquals("Path is required", result.error());
    }

    @Test
    void ensureDirectoryPath_returnsOkForExistingDirectory(@TempDir Path tempDir) {
        var result = ToolUtils.ensureDirectoryPath(tempDir.toString());
        assertTrue(result.ok());
        assertNull(result.error());
    }

    @Test
    void ensureDirectoryPath_returnsErrorForFile(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        var result = ToolUtils.ensureDirectoryPath(file.toString());
        assertFalse(result.ok());
        assertTrue(result.error().contains("not a directory"));
    }

    @Test
    void truncateText_doesNotTruncateShortText() {
        var result = ToolUtils.truncateText("abc", 10);
        assertEquals("abc", result.text());
        assertFalse(result.truncated());
    }

    @Test
    void truncateText_truncatesLongTextWithSuffix() {
        var result = ToolUtils.truncateText("abcdef", 3);
        assertTrue(result.truncated());
        assertTrue(result.text().startsWith("abc"));
        assertTrue(result.text().contains("truncated"));
    }

    @Test
    void truncateText_handlesNullInput() {
        var result = ToolUtils.truncateText(null, 10);
        assertEquals("", result.text());
        assertFalse(result.truncated());
    }
}