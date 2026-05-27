package com.aether.coding;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface PermissionService {

    enum Decision { ALLOW, DENY, ASK }

    record PermissionRequest(
        String toolName,
        java.util.Map<String, Object> input,
        String description
    ) {}

    CompletableFuture<Decision> check(PermissionRequest request);

    PermissionService ALLOW_ALL = request ->
        CompletableFuture.completedFuture(Decision.ALLOW);

    static PermissionService allowList(Set<String> allowedTools) {
        return request -> {
            if (allowedTools.contains(request.toolName())) {
                return CompletableFuture.completedFuture(Decision.ALLOW);
            }
            return CompletableFuture.completedFuture(Decision.ASK);
        };
    }
}