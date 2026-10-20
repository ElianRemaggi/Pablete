package com.instituto.siga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "mesa_examen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Turno turno;

    @Column(name = "cupo_maximo", nullable = false)
    private int cupoMaximo;

    @Column(length = 150)
    private String profesor;

    /** Hora de inicio del examen. Nullable: las mesas previas a la primera entrega no la tenian. */
    @Column(name = "hora")
    private LocalTime hora;

    @Column(length = 20)
    private String periodo;

    @Column(length = 100)
    private String aula;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Modalidad modalidad;

    /** Ventana propia de la mesa. Si es null se usa la ventana general de ConfiguracionInstitucional. */
    @Column(name = "apertura_inscripcion")
    private LocalDateTime aperturaInscripcion;

    @Column(name = "cierre_inscripcion")
    private LocalDateTime cierreInscripcion;

    /** El default PUBLICADA preserva el comportamiento de las mesas ya existentes (todas eran inscribibles);
     * las mesas nuevas creadas por la API nacen en BORRADOR (ver MesaExamenServiceImpl). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoMesa estado = EstadoMesa.PUBLICADA;
}
