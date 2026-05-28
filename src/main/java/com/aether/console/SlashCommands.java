package com.aether.console;

import com.aether.foundation.skills.SkillCatalog;
import com.aether.foundation.skills.SkillDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Parses console lines that start with {@code /} (exit, help, skill invocation). */
public final class SlashCommands {

    private static final Pattern SKILL_SLASH = Pattern.compile("^/([^\\s]+)(?:\\s|$)");

    private SlashCommands() {}

    public record Submission(String text, String requestedSkillName) {}

    public static List<SkillDescriptor> listAvailableSkills(SkillCatalog catalog) {
        try {
            return catalog.listSkills();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static Submission parseInput(String line, List<SkillDescriptor> skills) {
        var trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new Submission(trimmed, null);
        }

        var builtin = resolveBuiltin(trimmed);
        if (builtin != null) {
            return builtin;
        }

        var m = SKILL_SLASH.matcher(trimmed);
        if (!m.find()) {
            return new Submission(trimmed, null);
        }
        var token = m.group(1);
        if (token == null) {
            return new Submission(trimmed, null);
        }
        for (var skill : skills) {
            if (skill.name().equalsIgnoreCase(token)) {
                return new Submission(trimmed, skill.name());
            }
        }
        return new Submission(trimmed, null);
    }

    private static Submission resolveBuiltin(String trimmed) {
        if ("/exit".equalsIgnoreCase(trimmed) || "/quit".equalsIgnoreCase(trimmed)) {
            return new Submission(trimmed, "__exit__");
        }
        if ("/clear".equalsIgnoreCase(trimmed)) {
            return new Submission(trimmed, "__clear__");
        }
        if ("/help".equalsIgnoreCase(trimmed) || trimmed.toLowerCase(Locale.ROOT).startsWith("/help ")) {
            return new Submission(trimmed, "__help__");
        }
        return null;
    }

    public static String formatHelp(List<SkillDescriptor> skills) {
        var lines = new ArrayList<String>();
        lines.add("Available slash commands");
        lines.add("");
        lines.add("Built-in:");
        lines.add("  /exit, /quit  — exit");
        lines.add("  /clear        — clear conversation history");
        lines.add("  /help         — this message");
        if (!skills.isEmpty()) {
            lines.add("");
            lines.add("Skills (invoke with /name, e.g. /coding-plan):");
            var sorted = new ArrayList<>(skills);
            sorted.sort(Comparator.comparing(SkillDescriptor::name));
            for (var s : sorted) {
                var desc = s.description();
                if (desc.length() > 120) {
                    desc = desc.substring(0, 117) + "...";
                }
                lines.add("  /" + s.name() + " — " + desc);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }
}
