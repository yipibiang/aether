package com.aether.foundation.skills;

/** Skill metadata from SKILL.md frontmatter (layer-neutral DTO). */
public record SkillDescriptor(String name, String description, String path) {}
