package com.instituto.siga.service;

import com.instituto.siga.dto.response.TasaAprobacionResponse;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.service.impl.ReporteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;

    private ReporteServiceImpl reporteService;

    @BeforeEach
    void setUp() {
        reporteService = new ReporteServiceImpl(estadoAcademicoMateriaRepository);
    }

    @Test
    void tasaAprobacionPorMateria_mapeaCadaFilaDelProyeccion() {
        when(estadoAcademicoMateriaRepository.calcularTasaAprobacionPorMateria()).thenReturn(List.of(
                new Object[] {1L, "Materia 1", 3L, 5L},
                new Object[] {2L, "Materia 2", 0L, 2L}));

        List<TasaAprobacionResponse> resultado = reporteService.tasaAprobacionPorMateria();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0)).isEqualTo(new TasaAprobacionResponse(1L, "Materia 1", 3L, 5L));
        assertThat(resultado.get(1)).isEqualTo(new TasaAprobacionResponse(2L, "Materia 2", 0L, 2L));
    }
}
