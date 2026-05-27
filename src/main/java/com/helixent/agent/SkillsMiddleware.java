package com.helixent.agent;

import com.helixent.foundation.models.ModelContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SkillsMiddleware implements AgentMiddleware {

    private final List<String> skillsDirs;

    public SkillsMiddleware(List<String> skillsDirs) {
        this.skillsDirs = skillsDirs != null ? skillsDirs : List.of();
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgentRun(AgentContextView agentContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var skills = ListSkills.listSkills(skillsDirs);
                return Map.of("skills", (Object) skills);
            } catch (Exception e) {
                return Map.of("skills", (Object) List.<SkillFrontmatter>of());
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(ModelContext modelContext, AgentContextView agentContext) {
        return CompletableFuture.supplyAsync(() -> {
            var skills = agentContext.skills();
            if (skills == null || skills.isEmpty()) return null;

            var requestedSkill = agentContext.requestedSkillName() != null
                ? skills.stream()
                    .filter(s -> s.name().equalsIgnoreCase(agentContext.requestedSkillName()))
                    .findFirst().orElse(null)
                : null;

            var skillsXML = skills.stream()
                .map(s -> "<skill name=\"" + s.name() + "\" path=\"" + s.path() + "\">\n" + s.description() + "\n</skill>")
                .collect(Collectors.joining("\n"));

            var explicitBlock = requestedSkill != null
                ? "<explicit_skill_invocation>\nThe user explicitly selected the skill \"" + requestedSkill.name()
                    + "\" from the slash command picker.\nYou must read the matching skill file at \""
                    + requestedSkill.path() + "\" before answering.\n</explicit_skill_invocation>\n"
                : "";

            var newPrompt = modelContext.prompt() + "\n<skill_system>\n<instructions>\n"
                + "You have access to skills that provide optimized workflows for specific tasks. "
                + "Each skill contains best practices, frameworks, and references to additional resources.\n\n"
                + "**Progressive Loading Pattern:**\n"
                + "1. When a user query matches a skill's use case, immediately call `read_file` on the skill's main file using the path attribute provided in the skill tag below\n"
                + "2. If an explicit requested skill is provided in the system context, load that skill first even if the user message is short\n"
                + "3. Read and understand the skill's workflow and instructions\n"
                + "4. The skill file contains references to external resources under the same folder\n"
                + "5. Load referenced resources only when needed during execution\n"
                + "6. Follow the skill's instructions precisely\n"
                + "</instructions>\n\n"
                + explicitBlock
                + "<skills>\n" + skillsXML + "\n</skills>\n</skill_system>";

            return Map.of("prompt", (Object) newPrompt);
        });
    }
}