package com.ecommerce.tiendaspring.models;

import java.math.BigDecimal;
import java.util.Objects;

public class CarritoItemDTO {
    private Producto producto;
    private int cantidad;
    private BigDecimal subtotal;

    // Constructores
    public CarritoItemDTO() {}

    public CarritoItemDTO(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }

    // Getters y Setters
    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        if (this.producto != null && this.producto.getPrecio() != null) {
            this.subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    // equals y hashCode para comparar por producto
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CarritoItemDTO that = (CarritoItemDTO) obj;
        Long thisId = (this.producto == null) ? null : this.producto.getId();
        Long thatId = (that.producto == null) ? null : that.producto.getId();
        return Objects.equals(thisId, thatId);
    }

    @Override
    public int hashCode() {
        Long id = (producto == null) ? null : producto.getId();
        return Objects.hash(id);
    }
}