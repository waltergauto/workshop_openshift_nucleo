package com.workshop.producto.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductoDto {

    /** Request para crear un producto */
    public static class Crear {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        public String nombre;

        @Size(max = 500)
        public String descripcion;

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        public BigDecimal precio;

        @NotNull(message = "El stock es obligatorio")
        @Min(0)
        public Integer stock;
    }

    /** Request para actualizar stock */
    public static class ActualizarStock {
        @NotNull(message = "La cantidad es obligatoria")
        public Integer cantidad;  // puede ser negativo (decrementar)
    }
}
