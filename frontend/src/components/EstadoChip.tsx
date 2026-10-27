import { Chip } from '@mui/material';

type Color = 'default' | 'success' | 'warning' | 'error' | 'info';

const COLORES: Record<string, Color> = {
  BORRADOR: 'default',
  PUBLICADA: 'success',
  CERRADA: 'warning',
  CANCELADA: 'error',
  INSCRIPTO: 'success',
  MODIFICADA: 'info',
  BAJA: 'error',
  PENDIENTE: 'default',
  APROBADA: 'success',
  DESAPROBADA: 'error',
  APROBADO: 'success',
  DESAPROBADO: 'error',
  AUSENTE: 'warning',
};

const ETIQUETAS: Record<string, string> = {
  BORRADOR: 'Borrador',
  PUBLICADA: 'Publicada',
  CERRADA: 'Cerrada',
  CANCELADA: 'Cancelada',
  INSCRIPTO: 'Inscripto',
  MODIFICADA: 'Modificada',
  BAJA: 'Dada de baja',
  PENDIENTE: 'Pendiente',
  APROBADA: 'Aprobada',
  DESAPROBADA: 'Desaprobada',
  APROBADO: 'Aprobado',
  DESAPROBADO: 'Desaprobado',
  AUSENTE: 'Ausente',
};

/** Chip de estado para mesas de examen y para inscripciones a finales. */
export function EstadoChip({ estado }: { estado: string }) {
  return <Chip size="small" label={ETIQUETAS[estado] ?? estado} color={COLORES[estado] ?? 'default'} />;
}
