package com.instituto.siga.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Clave compuesta de PlanMateria (@IdClass), literal de docs/03-entidades-dto.md SS2. */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PlanMateriaId implements Serializable {

    private Long plan;
    private Long materia;
}
