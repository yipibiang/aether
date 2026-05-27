package com.aether.cli.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Resolves the charset for Windows / POSIX consoles when {@code file.encoding} is UTF-8 but
 * {@code stdout.encoding} / {@code native.encoding} still reflect the host console (often GBK).
 * <p>
 * Override with environment variable {@code AETHER_CONSOLE_CHARSET} (e.g. {@code UTF-8} for
 * Windows Terminal with UTF-8 code page).
 */
public final class ConsoleCharset {

    private ConsoleCharset() {}

    /**
     * Charset for stdin, stdout, and stderr so typed Chinese and model output match the console.
     */
    public static Charset forConsoleIo() {
        var override = System.getenv("AETHER_CONSOLE_CHARSET");
        if (override == null || override.isBlank()) {
            override = System.getProperty("aether.console.charset");
        }
        if (override != null && !override.isBlank()) {
            return Charset.forName(override.trim());
        }
        var outEnc = System.getProperty("stdout.encoding");
        if (outEnc != null && !outEnc.isBlank()) {
            try {
                return Charset.forName(outEnc);
            } catch (Exception ignored) {
            }
        }
        var nativeEnc = System.getProperty("native.encoding");
        if (nativeEnc != null && !nativeEnc.isBlank()) {
            try {
                return Charset.forName(nativeEnc);
            } catch (Exception ignored) {
            }
        }
        return Charset.defaultCharset() != null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
    }
}
