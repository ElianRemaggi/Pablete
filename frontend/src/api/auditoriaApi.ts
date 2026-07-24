import { httpClient } from './httpClient';

export interface AuditoriaResponse {
  id: number;
  usuarioId: number | null;
  usuarioNombre: string | null;
  accion: string;
  entidad: string;
  entidadId: number | null;
  detalle: string;
  ip: string;
  fecha: string;
}

interface PageResponse<T> {
  content: T[];
}

export interface FiltrosAuditoria {
  accion?: string;
  entidad?: string;
}

export async function listarAuditoria(filtros: FiltrosAuditoria): Promise<AuditoriaResponse[]> {
  const params: Record<string, string> = { size: '100' };
  if (filtros.accion) params.accion = filtros.accion;
  if (filtros.entidad) params.entidad = filtros.entidad;

  const { data } = await httpClient.get<PageResponse<AuditoriaResponse>>('/auditoria', { params });
  return data.content;
}
