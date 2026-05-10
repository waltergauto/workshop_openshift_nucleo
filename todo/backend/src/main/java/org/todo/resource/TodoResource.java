package org.todo.resource;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.todo.dto.CreateTodoRequest;
import org.todo.dto.TodoResponse;
import org.todo.dto.UpdateTodoRequest;
import org.todo.entity.Todo;
import org.todo.entity.TodoStatus;
import org.todo.repository.TodoRepository;

import java.net.URI;
import java.util.List;

@Path("/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TodoResource {

    @Inject
    TodoRepository todoRepository;

    @GET
    public Uni<List<TodoResponse>> getAllTodos() {
        return todoRepository.findAllActive()
            .map(todos -> todos.stream()
                .map(TodoResponse::from)
                .toList());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getTodo(@PathParam("id") Long id) {
        return todoRepository.findById(id)
            .map(todo -> {
                if (todo == null || todo.status == TodoStatus.DELETED) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                return Response.ok(TodoResponse.from(todo)).build();
            });
    }

    @POST
    @WithTransaction
    public Uni<Response> createTodo(@Valid CreateTodoRequest request) {
        Todo todo = new Todo();
        todo.title = request.title();
        todo.description = request.description();
        todo.status = TodoStatus.PENDING;

        return todoRepository.persist(todo)
            .map(persisted -> Response
                .created(URI.create("/todos/" + persisted.id))
                .entity(TodoResponse.from(persisted))
                .build());
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    public Uni<Response> updateTodo(@PathParam("id") Long id, @Valid UpdateTodoRequest request) {
        return todoRepository.findById(id)
            .flatMap(todo -> {
                if (todo == null || todo.status == TodoStatus.DELETED) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND).build()
                    );
                }
                if (request.title() != null) {
                    todo.title = request.title();
                }
                if (request.description() != null) {
                    todo.description = request.description();
                }
                if (request.status() != null) {
                    todo.status = request.status();
                }
                return todoRepository.persist(todo)
                    .map(updated -> Response.ok(TodoResponse.from(updated)).build());
            });
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    public Uni<Response> deleteTodo(@PathParam("id") Long id) {
        return todoRepository.findById(id)
            .flatMap(todo -> {
                if (todo == null || todo.status == TodoStatus.DELETED) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND).build()
                    );
                }
                todo.status = TodoStatus.DELETED;
                return todoRepository.persist(todo)
                    .map(updated -> Response.ok(TodoResponse.from(updated)).build());
            });
    }
}
