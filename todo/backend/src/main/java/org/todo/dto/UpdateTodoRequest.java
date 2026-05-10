package org.todo.dto;

import jakarta.validation.constraints.Size;
import org.todo.entity.TodoStatus;

public record UpdateTodoRequest(
    @Size(max = 255, message = "Title must be less than 255 characters")
    String title,
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    String description,
    
    TodoStatus status
) {}
