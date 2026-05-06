package com.workshop.todo.controller;

import com.workshop.todo.model.Todo;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TodoResource {

    @Inject
    Tracer tracer;

    @GET
    public List<Todo> getAllTodos() {
        Span span = tracer.spanBuilder("GET /todos").startSpan();
        try {
            span.setAttribute("http.method", "GET");
            return Todo.listAll();
        } finally {
            span.end();
        }
    }

    @POST
    @Transactional
    public Response createTodo(Todo todo) {
        Span span = tracer.spanBuilder("POST /todos").startSpan();
        try {
            span.setAttribute("http.method", "POST");
            todo.persist();
            return Response.status(Response.Status.CREATED).entity(todo).build();
        } finally {
            span.end();
        }
    }

    @GET
    @Path("/{id}")
    public Response getTodo(@PathParam("id") Long id) {
        Todo todo = Todo.findById(id);
        if (todo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(todo).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateTodo(@PathParam("id") Long id, Todo updated) {
        Todo todo = Todo.findById(id);
        if (todo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        todo.title = updated.title;
        todo.description = updated.description;
        todo.completed = updated.completed;
        return Response.ok(todo).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteTodo(@PathParam("id") Long id) {
        boolean deleted = Todo.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}