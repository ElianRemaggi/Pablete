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
};

const ETIQUETAS: Record<string, string> = {
  BORRADOR: 'Borrador',
  PUBLICADA: 'Publicada',
  CERRADA: 'Cerrada',
  CANCELADA: 'Cancelada',
  INSCRIPTO: 'Inscripto',
  MODIFICADA: 'Modificada',
  BAJA: 'Dada de baja',
};

/** Chip de estado para mesas de examen y para inscripciones a finales. */
export function EstadoChip({ estado }: { estado: string }) {
  return <Chip size="small" label={ETIQUETAS[estado] ?? estado} color={COLORES[estado] ?? 'default'} />;
}
