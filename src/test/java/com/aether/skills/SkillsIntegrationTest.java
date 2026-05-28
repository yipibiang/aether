package com.aether.skills;

import com.aether.coding.agents.LeadAgent;
import com.aether.foundation.skills.SkillDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SkillsIntegrationTest {

    @Test
    void listSkills_findsBundledCodingAndResearchPlans() throws Exception {
        var cwd = Path.of("").toAbsolutePath().normalize().toString();
        var catalog = new FileSystemSkillCatalog(LeadAgent.defaultSkillsDirs(cwd));
        var skills = catalog.listSkills();
        var names = skills.stream().map(SkillDescriptor::name).sorted().toList();
        assertTrue(names.contains("coding-plan"), "expected coding-plan in " + names);
        assertTrue(names.contains("deep-research-plan"), "expected deep-research-plan in " + names);
    }
}
