package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamaciones")
public class Reclamacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String tipo; // QUEJA o RECLAMO

    @NotBlank
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @NotBlank
    @Size(max = 20)
    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @NotBlank
    @Size(max = 255)
    @Column(name = "nombre_consumidor", nullable = false)
    private String nombreConsumidor;

    @Size(max = 500)
    @Column(length = 500)
    private String domicilio;

    @NotBlank
    @Email
    @Column(nullable = false, length = 255)
    private String correo;

    @Size(max = 20)
    @Column(length = 20)
    private String telefono;

    @NotBlank
    @Column(name = "bien_contratado", nullable = false, columnDefinition = "TEXT")
    private String bienContratado;

    @Column(name = "monto_reclamado", precision = 19, scale = 2)
    private BigDecimal montoReclamado;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String pedido;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
    }

    // Getters and Setters

    public Long getId() { return id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getNombreConsumidor() { return nombreConsumidor; }
    public void setNombreConsumidor(String nombreConsumidor) { this.nombreConsumidor = nombreConsumidor; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getBienContratado() { return bienContratado; }
    public void setBienContratado(String bienContratado) { this.bienContratado = bienContratado; }

    public BigDecimal getMontoReclamado() { return montoReclamado; }
    public void setMontoReclamado(BigDecimal montoReclamado) { this.montoReclamado = montoReclamado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPedido() { return pedido; }
    public void setPedido(String pedido) { this.pedido = pedido; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
