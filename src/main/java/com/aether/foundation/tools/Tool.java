package com.aether.foundation.tools;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工具接口 — Agent 可调用的"动作"抽象。
 *
 * <h3>设计要点</h3>
 * {@link #toJsonSchema()} 供模型理解参数；{@link #invoke} 异步执行，支持并行调用。
 * {@code signal} 预留取消（当前未实现）。
 */
public interface Tool {

    String name();

    String description();

    Map<String, Object> toJsonSchema();

    CompletableFuture<Object> invoke(Map<String, Object> input, Object signal);
}