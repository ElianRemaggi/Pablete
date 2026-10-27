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

/**
 * docs/02 modela intento_final como el historico de intentos, pensado para completarse cuando
 * se rinde el final (resultado). Como todavia no existe un modulo de "registrar resultado",
 * InscripcionFinalServiceImpl crea esta fila (resultado=null) al inscribirse — anotarse ya
 * consume un intento — y la borra si el alumno da de baja antes de la mesa.
 */
@Entity
@Table(name = "intento_final")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_final_id", nullable = false)
    private InscripcionFinal inscripcionFinal;

    @Column(name = "nro_intento", nullable = false)
    private int nroIntento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoFinal resultado;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Nota numerica (1-10) del final rendido; null si no hay resultado o fue AUSENTE. */
    @Column
    private Integer nota;

    @Column(name = "fecha_resultado")
    private java.time.LocalDateTime fechaResultado;

    @Column(name = "calificado_por")
    private Long calificadoPor;
}
