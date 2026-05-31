package com.workshop.pedido.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos", schema = "pedidos")
public class Pedido extends PanacheEntity {

    public enum Estado {
        PENDIENTE, CONFIRMADO, CANCELADO
    }

    @NotNull(message = "El ID de producto es obligatorio")
    @Column(name = "producto_id", nullable = false)
    public Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que 0")
    @Column(nullable = false)
    public Integer cantidad;

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    public BigDecimal precioUnitario;

    @Column(name = "total", precision = 10, scale = 2)
    public BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Estado estado;

    @Size(max = 200)
    @Column(name = "cliente_nombre", length = 200)
    public String clienteNombre;

    @Column(name = "creado_en", nullable = false, updatable = false)
    public LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    public LocalDateTime actualizadoEn;

    @PrePersist
    void onCreate() {
        creadoEn      = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
        if (estado == null) {
            estado = Estado.PENDIENTE;
        }
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
