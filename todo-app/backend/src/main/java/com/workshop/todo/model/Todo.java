package com.workshop.todo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "todos")
public class Todo extends PanacheEntity {

    @Column(length = 255, nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "completed")
    public boolean completed = false;

    public static java.util.List<Todo> findAllTodos() {
        return listAll();
    }

    public static Todo findById(Long id) {
        return find("id", id).firstResult();
    }
}