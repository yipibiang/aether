package com.aether.console;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsoleCharsetTest {

    @Test
    void forConsoleIo_returnsNonNull() {
        assertNotNull(ConsoleCharset.forConsoleIo());
    }
}
