package com.instituto.siga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "correlatividad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Correlatividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne
    @JoinColumn(name = "materia_requerida_id", nullable = false)
    private Materia materiaRequerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_requisito", nullable = false, length = 20)
    private TipoRequisito tipoRequisito;
}
