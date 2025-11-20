package com.ecommerce.tiendaspring.models;

public class StockUpdate {
    private Long productoId;
    private int stock;
    private int stockReservado;

    public StockUpdate() {}

    public StockUpdate(Long productoId, int stock, int stockReservado) {
        this.productoId = productoId;
        this.stock = stock;
        this.stockReservado = stockReservado;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockReservado() {
        return stockReservado;
    }

    public void setStockReservado(int stockReservado) {
        this.stockReservado = stockReservado;
    }
}
