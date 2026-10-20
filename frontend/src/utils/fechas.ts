/** Fecha sin hora ("2026-11-20") -> "20/11/2026". No pasa por Date: new Date('2026-11-20') se interpreta
 * en UTC y en Argentina (UTC-3) mostraria el dia anterior. */
export function formatearFecha(fecha: string | null | undefined): string {
  if (!fecha) return '—';
  const [anio, mes, dia] = fecha.slice(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
}

export function formatearHora(hora: string | null | undefined): string {
  return hora ? hora.slice(0, 5) : '—';
}

export function formatearFechaHora(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });
}

/** LocalDateTime del backend ("2026-11-20T10:00:00") -> valor para <input type="datetime-local">. */
export function aInputDatetime(iso: string | null | undefined): string {
  return iso ? iso.slice(0, 16) : '';
}

export const ETIQUETA_TURNO: Record<string, string> = { MANANA: 'Mañana', TARDE: 'Tarde', NOCHE: 'Noche' };
