package com.workshop.pedido.service;

import com.workshop.pedido.client.NotificacionClient;
import com.workshop.pedido.client.ProductoClient;
import com.workshop.pedido.model.Pedido;
import com.workshop.pedido.model.PedidoDto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PedidoService {

    private static final Logger LOG = Logger.getLogger(PedidoService.class);

    @Inject
    Tracer tracer;

    @Inject
    @RestClient
    ProductoClient productoClient;

    @Inject
    @RestClient
    NotificacionClient notificacionClient;

    public List<Pedido> listar() {
        return Pedido.listAll();
    }

    public Pedido buscarPorId(Long id) {
        Pedido pedido = Pedido.findById(id);
        if (pedido == null) {
            throw new NotFoundException("Pedido no encontrado: " + id);
        }
        return pedido;
    }

    /**
     * Flujo principal — genera el árbol de trazas más interesante:
     *
     *  POST /pedidos  (pedido-api)
     *    └─ GET /productos/{id}           (producto-api)  ← span automático OTel
     *    └─ PUT /productos/{id}/stock     (producto-api)  ← span automático OTel
     *    └─ POST /notificaciones          (notificacion-api) ← span automático OTel
     */
    @Transactional
    public Pedido crear(PedidoDto.Crear dto) {
        Span span = tracer.spanBuilder("pedido.crear")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("pedido.producto_id", dto.productoId);
            span.setAttribute("pedido.cantidad", dto.cantidad);
            span.setAttribute("pedido.cliente", dto.clienteNombre != null ? dto.clienteNombre : "anonimo");

            // 1. Consultar producto (span hijo automático via quarkus-opentelemetry)
            LOG.infof("Consultando producto %d en producto-api", dto.productoId);
            PedidoDto.ProductoInfo producto;
            try {
                producto = productoClient.buscarPorId(dto.productoId);
            } catch (WebApplicationException e) {
                span.setStatus(StatusCode.ERROR, "Producto no encontrado");
                throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\":\"Producto no encontrado: " + dto.productoId + "\"}")
                            .build()
                );
            }

            span.setAttribute("producto.nombre", producto.nombre);
            span.setAttribute("producto.stock_disponible", producto.stock);

            // 2. Validar stock
            if (producto.stock < dto.cantidad) {
                span.setStatus(StatusCode.ERROR, "Stock insuficiente");
                throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\":\"Stock insuficiente\",\"stock_disponible\":"
                                    + producto.stock + ",\"cantidad_solicitada\":" + dto.cantidad + "}")
                            .build()
                );
            }

            // 3. Persistir pedido
            Pedido pedido = new Pedido();
            pedido.productoId     = dto.productoId;
            pedido.cantidad       = dto.cantidad;
            pedido.precioUnitario = producto.precio;
            pedido.total          = producto.precio.multiply(BigDecimal.valueOf(dto.cantidad));
            pedido.clienteNombre  = dto.clienteNombre;
            pedido.estado         = Pedido.Estado.CONFIRMADO;
            pedido.persist();

            span.setAttribute("pedido.id", pedido.id);
            span.setAttribute("pedido.total", pedido.total.toString());

            // 4. Descontar stock (span hijo automático)
            LOG.infof("Descontando %d unidades del producto %d", dto.cantidad, dto.productoId);
            productoClient.actualizarStock(dto.productoId, Map.of("cantidad", -dto.cantidad));

            // 5. Notificar (span hijo automático — tercer nivel de traza)
            LOG.infof("Enviando notificación para pedido %d", pedido.id);
            try {
                notificacionClient.enviar(Map.of(
                    "pedidoId",      pedido.id,
                    "productoId",    pedido.productoId,
                    "productoNombre",producto.nombre,
                    "cantidad",      pedido.cantidad,
                    "total",         pedido.total.toString(),
                    "cliente",       pedido.clienteNombre != null ? pedido.clienteNombre : "anonimo",
                    "tipo",          "PEDIDO_CONFIRMADO"
                ));
            } catch (Exception e) {
                // La notificación es best-effort: no falla el pedido
                LOG.warnf("No se pudo enviar notificación para pedido %d: %s", pedido.id, e.getMessage());
                span.addEvent("notificacion.fallida");
            }

            LOG.infof("Pedido %d creado correctamente", pedido.id);
            return pedido;

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional
    public Pedido actualizarEstado(Long id, PedidoDto.ActualizarEstado dto) {
        Pedido pedido = Pedido.findById(id);
        if (pedido == null) {
            throw new NotFoundException("Pedido no encontrado: " + id);
        }
        pedido.estado = dto.estado;
        return pedido;
    }
}
