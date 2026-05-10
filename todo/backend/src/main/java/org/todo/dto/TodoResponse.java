package org.todo.dto;

import org.todo.entity.Todo;
import org.todo.entity.TodoStatus;
import java.time.LocalDateTime;

public record TodoResponse(
    Long id,
    String title,
    String description,
    TodoStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
            todo.id,
            todo.title,
            todo.description,
            todo.status,
            todo.createdAt,
            todo.updatedAt
        );
    }
}
