package org.todo.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.todo.entity.Todo;
import org.todo.entity.TodoStatus;

import java.util.List;

@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {

    public Uni<List<Todo>> findAllActive() {
        return list("status != ?1", TodoStatus.DELETED);
    }

    public Uni<List<Todo>> findByStatus(TodoStatus status) {
        return list("status", status);
    }

    public Uni<Long> deleteByIdReturning(Long id) {
        return update("status = ?1 where id = ?2", TodoStatus.DELETED, id)
            .chain(() -> Uni.createFrom().item(id));
    }
}
