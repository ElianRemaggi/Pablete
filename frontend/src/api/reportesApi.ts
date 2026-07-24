import { httpClient } from './httpClient';

export interface TasaAprobacionResponse {
  materiaId: number;
  materiaNombre: string;
  aprobadas: number;
  total: number;
}

export async function obtenerTasaAprobacionPorMateria(): Promise<TasaAprobacionResponse[]> {
  const { data } = await httpClient.get<TasaAprobacionResponse[]>('/reportes/tasa-aprobacion-por-materia');
  return data;
}
