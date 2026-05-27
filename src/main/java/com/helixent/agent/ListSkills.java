package com.helixent.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ListSkills {

    private ListSkills() {}

    public static List<SkillFrontmatter> listSkills(List<String> skillsDirs) throws IOException {
        if (skillsDirs == null || skillsDirs.isEmpty()) {
            skillsDirs = List.of(Paths.get("").toAbsolutePath().resolve("skills").toString());
        }

        var skills = new ArrayList<SkillFrontmatter>();
        var seenSkillFiles = new LinkedHashSet<String>();

        for (var skillsDir : skillsDirs) {
            var resolved = skillsDir;
            if (resolved.startsWith("~")) {
                resolved = Paths.get(System.getProperty("user.home"), resolved.substring(1)).toString();
            }
            var dir = Path.of(resolved);
            if (!Files.exists(dir)) continue;

            java.nio.file.DirectoryStream<Path> stream;
            try {
                stream = Files.newDirectoryStream(dir);
            } catch (IOException e) {
                continue;
            }

            try (stream) {
                for (var folder : stream) {
                    if (!Files.isDirectory(folder)) continue;
                    var skillFilePath = folder.resolve("SKILL.md");
                    if (seenSkillFiles.contains(skillFilePath.toString())) continue;
                    if (!Files.exists(skillFilePath)) continue;

                    seenSkillFiles.add(skillFilePath.toString());
                    var frontmatter = SkillReader.readSkillFrontMatter(skillFilePath);
                    skills.add(frontmatter);
                }
            }
        }

        return skills;
    }
}