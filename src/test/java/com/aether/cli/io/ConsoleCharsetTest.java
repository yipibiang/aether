package com.aether.cli.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsoleCharsetTest {

    @Test
    void forConsoleIo_returnsNonNull() {
        assertNotNull(ConsoleCharset.forConsoleIo());
    }
}
