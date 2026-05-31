package com.workshop.notificacion.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", schema = "notificaciones")
public class Notificacion extends PanacheEntity {

    public enum Tipo {
        PEDIDO_CONFIRMADO, PEDIDO_CANCELADO, STOCK_BAJO
    }

    public enum Canal {
        LOG, EMAIL, SMS
    }

    @Column(name = "pedido_id")
    public Long pedidoId;

    @Column(name = "producto_id")
    public Long productoId;

    @Column(name = "producto_nombre", length = 150)
    public String productoNombre;

    @Column(name = "cliente_nombre", length = 200)
    public String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Canal canal;

    @Column(length = 1000)
    public String mensaje;

    @Column(name = "enviada_en", nullable = false, updatable = false)
    public LocalDateTime enviadaEn;

    @PrePersist
    void onCreate() {
        enviadaEn = LocalDateTime.now();
        if (canal == null) {
            canal = Canal.LOG;
        }
    }
}
