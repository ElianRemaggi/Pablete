import type { TipoEventoCalendario } from '../../api/calendarioApi';

/**
 * Colores deliberadamente alejados de #05acec (color institucional) para que no se confundan
 * con la identidad visual del sistema — ver §5 del prompt de actualización.
 */
export const COLORES_TIPO_EVENTO: Record<TipoEventoCalendario, string> = {
  CURSADA: '#3f51b5',
  FINAL: '#2e7d32',
  INSCRIPCION: '#f9a825',
  EVENTO_ACADEMICO: '#7b1fa2',
  FECHA_LIMITE: '#c62828',
};

export const ETIQUETAS_TIPO_EVENTO: Record<TipoEventoCalendario, string> = {
  CURSADA: 'Cursada',
  FINAL: 'Final',
  INSCRIPCION: 'Inscripción',
  EVENTO_ACADEMICO: 'Evento académico',
  FECHA_LIMITE: 'Fecha límite',
};
