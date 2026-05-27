package com.helixent.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SkillReader {

    private static final Pattern FRONTMATTER_RE = Pattern.compile("^---\\s*\\n([\\s\\S]*?)\\n---");

    private SkillReader() {}

    public static SkillFrontmatter readSkillFrontMatter(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File " + path + " does not exist");
        }
        var content = Files.readString(path);
        var matcher = FRONTMATTER_RE.matcher(content);
        Map<String, Object> data = new LinkedHashMap<>();
        if (matcher.find()) {
            var yamlBlock = matcher.group(1);
            data = parseSimpleYaml(yamlBlock);
        }
        var name = (String) data.getOrDefault("name", path.getParent().getFileName().toString());
        var description = (String) data.getOrDefault("description", "");
        return new SkillFrontmatter(name, description, path.toString());
    }

    private static Map<String, Object> parseSimpleYaml(String yaml) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var line : yaml.split("\n")) {
            var colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                var key = line.substring(0, colonIdx).trim();
                var value = line.substring(colonIdx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        }
        return result;
    }
}