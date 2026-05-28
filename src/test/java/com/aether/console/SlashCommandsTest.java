package com.aether.console;

import com.aether.foundation.skills.SkillDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlashCommandsTest {

    @Test
    void parseInput_recognizesSkillSlashCommand() {
        var skills = List.of(new SkillDescriptor("coding-plan", "Plan mode", "/tmp/coding-plan/SKILL.md"));
        var submission = SlashCommands.parseInput("/coding-plan refactor auth", skills);
        assertEquals("coding-plan", submission.requestedSkillName());
        assertEquals("/coding-plan refactor auth", submission.text());
    }

    @Test
    void parseInput_plainTextHasNoSkill() {
        var skills = List.of(new SkillDescriptor("coding-plan", "d", "/p/SKILL.md"));
        var submission = SlashCommands.parseInput("hello", skills);
        assertNull(submission.requestedSkillName());
    }
}
