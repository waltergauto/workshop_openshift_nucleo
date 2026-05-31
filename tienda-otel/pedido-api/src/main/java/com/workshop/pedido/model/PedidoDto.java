package com.workshop.pedido.model;

import jakarta.validation.constraints.*;

public class PedidoDto {

    /** Request para crear un pedido */
    public static class Crear {
        @NotNull(message = "El productoId es obligatorio")
        public Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que 0")
        public Integer cantidad;

        @Size(max = 200)
        public String clienteNombre;
    }

    /** Request para actualizar estado */
    public static class ActualizarEstado {
        @NotNull(message = "El estado es obligatorio")
        public Pedido.Estado estado;
    }

    /** Representación del producto obtenida de producto-api */
    public static class ProductoInfo {
        public Long id;
        public String nombre;
        public java.math.BigDecimal precio;
        public Integer stock;
    }
}
