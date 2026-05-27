package com.helixent.agent.todos;

/**
 * JDK16 record：单个待办事项（id、内容、状态）。
 */
public record TodoItem(String id, String content, TodoStatus status) {}