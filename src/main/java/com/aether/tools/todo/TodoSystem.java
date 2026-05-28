package com.aether.tools.todo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TodoSystem {

    private static final int STEPS_SINCE_WRITE = 10;
    private static final int STEPS_BETWEEN_REMINDERS = 10;

    private final List<TodoItem> store = new ArrayList<>();
    private int stepsSinceLastWrite = Integer.MAX_VALUE;
    private int stepsSinceLastReminder = Integer.MAX_VALUE;

    @SuppressWarnings("unchecked")
    void applyWrite(List<Map<String, Object>> todos, boolean merge) {
        if (merge) {
            for (Map<String, Object> item : todos) {
                String id = (String) item.get("id");
                String content = (String) item.get("content");
                TodoStatus status = TodoStatus.valueOf((String) item.get("status"));
                TodoItem todoItem = new TodoItem(id, content, status);

                int idx = -1;
                for (int i = 0; i < store.size(); i++) {
                    if (store.get(i).id().equals(id)) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    store.set(idx, todoItem);
                } else {
                    store.add(todoItem);
                }
            }
        } else {
            store.clear();
            for (Map<String, Object> item : todos) {
                String id = (String) item.get("id");
                String content = (String) item.get("content");
                TodoStatus status = TodoStatus.valueOf((String) item.get("status"));
                store.add(new TodoItem(id, content, status));
            }
        }
        stepsSinceLastWrite = 0;
    }

    void onBeforeModelStep() {
        stepsSinceLastWrite++;
        stepsSinceLastReminder++;
    }

    void onTodoWriteUsed() {
        stepsSinceLastWrite = 0;
    }

    String reminderPromptSuffix() {
        if (store.isEmpty()
            || stepsSinceLastWrite < STEPS_SINCE_WRITE
            || stepsSinceLastReminder < STEPS_BETWEEN_REMINDERS) {
            return null;
        }
        stepsSinceLastReminder = 0;
        return formatReminder();
    }

    String formatSummary() {
        int pending = 0, inProgress = 0, completed = 0, cancelled = 0;
        for (TodoItem t : store) {
            switch (t.status()) {
                case pending -> pending++;
                case in_progress -> inProgress++;
                case completed -> completed++;
                case cancelled -> cancelled++;
            }
        }
        List<String> parts = new ArrayList<>();
        if (pending > 0) parts.add(pending + " pending");
        if (inProgress > 0) parts.add(inProgress + " in_progress");
        if (completed > 0) parts.add(completed + " completed");
        if (cancelled > 0) parts.add(cancelled + " cancelled");
        return "Todo list updated. " + store.size() + " items: " + String.join(", ", parts) + ".";
    }

    private String formatReminder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < store.size(); i++) {
            TodoItem t = store.get(i);
            sb.append(i + 1).append(". [").append(t.status()).append("] ").append(t.content()).append("\n");
        }
        return "\n<todo_reminder>\n" +
            "The todo_write tool hasn't been used recently. If you're working on tasks that benefit from tracking, " +
            "consider updating your todo list. Only use it if relevant to the current work. " +
            "Here are the current items:\n\n" +
            sb +
            "</todo_reminder>";
    }
}
