package com.instituto.siga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Fila unica (id siempre 1), ver CHECK(id = 1) en docs/02-modelo-datos.md. */
@Entity
@Table(name = "configuracion_institucional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionInstitucional {

    @Id
    private Integer id;

    @Column(name = "fecha_apertura_cursadas", nullable = false)
    private LocalDateTime fechaAperturaCursadas;

    @Column(name = "fecha_cierre_cursadas", nullable = false)
    private LocalDateTime fechaCierreCursadas;

    @Column(name = "fecha_apertura_finales", nullable = false)
    private LocalDateTime fechaAperturaFinales;

    @Column(name = "fecha_cierre_finales", nullable = false)
    private LocalDateTime fechaCierreFinales;

    @Column(name = "limite_intentos_final", nullable = false)
    private int limiteIntentosFinal;

    @Column(name = "horas_bloqueo_final", nullable = false)
    private int horasBloqueoFinal;
}
