package com.helixent.foundation.tools;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工具接口 — Agent 可调用的"动作"抽象。
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>{@link #toJsonSchema()} 返回工具的 JSON Schema 定义，供模型理解工具参数</li>
 *   <li>{@link #invoke(Map, Object)} 异步执行工具，返回 {@link CompletableFuture}。
 *       JDK8 中 CompletableFuture 已存在，这里用它支持并行工具调用</li>
 *   <li>{@code signal} 参数预留用于取消操作（当前未实现）</li>
 * </ul>
 */
public interface Tool {

    String name();

    String description();

    Map<String, Object> toJsonSchema();

    CompletableFuture<Object> invoke(Map<String, Object> input, Object signal);
}