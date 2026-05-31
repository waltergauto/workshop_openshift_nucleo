package com.workshop.notificacion.service;

import com.workshop.notificacion.model.Notificacion;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class);

    @Inject
    Tracer tracer;

    public List<Notificacion> listar() {
        return Notificacion.listAll();
    }

    @Transactional
    public Notificacion registrar(Map<String, Object> payload) {
        Span span = tracer.spanBuilder("notificacion.registrar")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Extraer campos del payload
            Long   pedidoId       = toLong(payload.get("pedidoId"));
            Long   productoId     = toLong(payload.get("productoId"));
            String productoNombre = toString(payload.get("productoNombre"));
            String cliente        = toString(payload.get("cliente"));
            String tipoStr        = toString(payload.get("tipo"));
            Integer cantidad      = toInt(payload.get("cantidad"));
            String total          = toString(payload.get("total"));

            span.setAttribute("notificacion.pedido_id", pedidoId != null ? pedidoId : -1L);
            span.setAttribute("notificacion.tipo", tipoStr != null ? tipoStr : "DESCONOCIDO");
            span.setAttribute("notificacion.cliente", cliente != null ? cliente : "anonimo");

            // Construir entidad
            Notificacion n = new Notificacion();
            n.pedidoId       = pedidoId;
            n.productoId     = productoId;
            n.productoNombre = productoNombre;
            n.clienteNombre  = cliente;
            n.canal          = Notificacion.Canal.LOG;

            try {
                n.tipo = Notificacion.Tipo.valueOf(tipoStr);
            } catch (Exception e) {
                n.tipo = Notificacion.Tipo.PEDIDO_CONFIRMADO;
            }

            n.mensaje = String.format(
                "Pedido #%d confirmado — Cliente: %s | Producto: %s x%d | Total: %s",
                pedidoId, cliente, productoNombre, cantidad, total
            );

            n.persist();

            span.setAttribute("notificacion.id", n.id);
            LOG.infof("Notificación registrada: id=%d tipo=%s pedido=%d", n.id, n.tipo, n.pedidoId);

            return n;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    // ─── Helpers de conversión segura ─────────────────────────────

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private String toString(Object o) {
        return o != null ? o.toString() : null;
    }
}
