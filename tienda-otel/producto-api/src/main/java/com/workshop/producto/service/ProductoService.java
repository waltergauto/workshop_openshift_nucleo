package com.workshop.producto.service;

import com.workshop.producto.model.Producto;
import com.workshop.producto.model.ProductoDto;
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
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ProductoService {

    private static final Logger LOG = Logger.getLogger(ProductoService.class);

    @Inject
    Tracer tracer;

    public List<Producto> listar() {
        LOG.info("Listando todos los productos");
        return Producto.listAll();
    }

    public Producto buscarPorId(Long id) {
        Producto producto = Producto.findById(id);
        if (producto == null) {
            throw new NotFoundException("Producto no encontrado: " + id);
        }
        return producto;
    }

    @Transactional
    public Producto crear(ProductoDto.Crear dto) {
        LOG.infof("Creando producto: %s", dto.nombre);

        Span span = tracer.spanBuilder("producto.crear")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("producto.nombre", dto.nombre);
            span.setAttribute("producto.precio", dto.precio.toString());
            span.setAttribute("producto.stock_inicial", dto.stock);

            Producto p = new Producto();
            p.nombre      = dto.nombre;
            p.descripcion = dto.descripcion;
            p.precio      = dto.precio;
            p.stock       = dto.stock;
            p.persist();

            span.setAttribute("producto.id", p.id);
            LOG.infof("Producto creado con ID=%d", p.id);
            return p;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional
    public Producto actualizarStock(Long id, ProductoDto.ActualizarStock dto) {
        Span span = tracer.spanBuilder("producto.actualizar-stock")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("producto.id", id);
            span.setAttribute("stock.delta", dto.cantidad);

            Producto producto = Producto.findById(id);
            if (producto == null) {
                span.setStatus(StatusCode.ERROR, "Producto no encontrado");
                throw new NotFoundException("Producto no encontrado: " + id);
            }

            int stockAnterior = producto.stock;
            int stockNuevo    = producto.stock + dto.cantidad;

            if (stockNuevo < 0) {
                span.setStatus(StatusCode.ERROR, "Stock insuficiente");
                throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\":\"Stock insuficiente\",\"stock_disponible\":" + producto.stock + "}")
                            .build()
                );
            }

            producto.stock = stockNuevo;
            span.setAttribute("stock.anterior", stockAnterior);
            span.setAttribute("stock.nuevo", stockNuevo);

            LOG.infof("Stock actualizado para producto %d: %d → %d", id, stockAnterior, stockNuevo);
            return producto;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
