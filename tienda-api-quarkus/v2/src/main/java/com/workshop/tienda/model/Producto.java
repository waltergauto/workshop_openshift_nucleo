package com.workshop.tienda.model;

public class Producto {

    public int    id;
    public String nombre;
    public double precio;
    public int    stock;
    public String categoria;   // nuevo en v2

    public Producto() {}

    public Producto(int id, String nombre, double precio, int stock, String categoria) {
        this.id        = id;
        this.nombre    = nombre;
        this.precio    = precio;
        this.stock     = stock;
        this.categoria = categoria;
    }
}
