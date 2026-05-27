package com.helixent.coding.permissions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ApprovalPersistence {

    CompletableFuture<Set<String>> loadAllowList(String cwd);

    CompletableFuture<Void> persistAllowedTool(String cwd, String toolName);

    static ApprovalPersistence fileBased(Path configDir) {
        return new FileBasedApprovalPersistence(configDir);
    }

    final class FileBasedApprovalPersistence implements ApprovalPersistence {
        private static final String ALLOW_FILE = "helixent-allowed-tools.txt";

        private final Path configDir;

        FileBasedApprovalPersistence(Path configDir) {
            this.configDir = configDir;
        }

        @Override
        public CompletableFuture<Set<String>> loadAllowList(String cwd) {
            return CompletableFuture.supplyAsync(() -> {
                var file = configDir.resolve(ALLOW_FILE);
                if (!Files.exists(file)) return Set.<String>of();
                try {
                    var lines = Files.readAllLines(file);
                    var allowed = new HashSet<String>();
                    for (var line : lines) {
                        var trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            allowed.add(trimmed);
                        }
                    }
                    return allowed;
                } catch (IOException e) {
                    return Set.<String>of();
                }
            });
        }

        @Override
        public CompletableFuture<Void> persistAllowedTool(String cwd, String toolName) {
            return CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(configDir);
                    Files.writeString(configDir.resolve(ALLOW_FILE),
                        toolName + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.err.println("[helixent] Could not persist allow for " + toolName + ": " + e.getMessage());
                }
            });
        }
    }
}