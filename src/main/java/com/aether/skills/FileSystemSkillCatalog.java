package com.aether.skills;

import com.aether.foundation.skills.SkillCatalog;
import com.aether.foundation.skills.SkillDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** {@link SkillCatalog} backed by SKILL.md files under configured directories. */
public final class FileSystemSkillCatalog implements SkillCatalog {

    private final List<String> skillsDirs;

    public FileSystemSkillCatalog(List<String> skillsDirs) {
        this.skillsDirs = skillsDirs != null ? List.copyOf(skillsDirs) : List.of();
    }

    @Override
    public List<SkillDescriptor> listSkills() throws IOException {
        var dirs = skillsDirs.isEmpty()
            ? List.of(Paths.get("").toAbsolutePath().resolve("skills").toString())
            : skillsDirs;

        var skills = new ArrayList<SkillDescriptor>();
        var seenSkillFiles = new LinkedHashSet<String>();

        for (var skillsDir : dirs) {
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
                    skills.add(SkillReader.readSkillFrontMatter(skillFilePath));
                }
            }
        }

        return skills;
    }
}
