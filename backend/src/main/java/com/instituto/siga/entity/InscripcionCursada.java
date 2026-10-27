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

import java.time.LocalDateTime;

@Entity
@Table(name = "inscripcion_cursada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionCursada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comision_id", nullable = false)
    private Comision comision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    /** Nota numerica (1-10) de la cursada; null mientras no se califique. No es la nota del final. */
    @Column(name = "nota_cursada")
    private Integer notaCursada;

    /** null equivale a PENDIENTE (sin calificar). */
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_cursada", length = 20)
    private ResultadoCursada resultadoCursada;

    @Column(name = "fecha_calificacion")
    private LocalDateTime fechaCalificacion;

    @Column(name = "calificado_por")
    private Long calificadoPor;
}
