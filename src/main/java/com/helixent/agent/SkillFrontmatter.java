package com.helixent.agent;

/**
 * JDK16 record：技能元数据（名称、描述、文件路径）。
 */
public record SkillFrontmatter(String name, String description, String path) {}