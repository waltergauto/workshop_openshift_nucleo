package com.example.backend;

public class Empleado {
    private Long id;
    private String nombre;
    private String puesto;
    private Double salario;

    public Empleado() {}

    public Empleado(String nombre, String puesto, Double salario) {
        this.nombre = nombre;
        this.puesto = puesto;
        this.salario = salario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPuesto() { return puesto; }
    public void setPuesto(String puesto) { this.puesto = puesto; }
    public Double getSalario() { return salario; }
    public void setSalario(Double salario) { this.salario = salario; }
}
