import { httpClient } from './httpClient';

export type Turno = 'MANANA' | 'TARDE' | 'NOCHE';
export type Modalidad = 'PRESENCIAL' | 'VIRTUAL';
export type EstadoMesa = 'BORRADOR' | 'PUBLICADA' | 'CERRADA' | 'CANCELADA';

export interface MesaExamenResponse {
  id: number;
  materiaId: number;
  materiaNombre: string;
  fecha: string;
  turno: string;
  cupoMaximo: number;
  profesor: string | null;
  hora: string | null;
  periodo: string | null;
  aula: string | null;
  modalidad: Modalidad | null;
  aperturaInscripcion: string | null;
  cierreInscripcion: string | null;
  estado: EstadoMesa;
  inscriptos: number;
  cupoDisponible: number;
  inscripcionAbierta: boolean;
}

export interface MesaExamenRequest {
  materiaId: number;
  fecha: string;
  turno: Turno;
  cupoMaximo: number;
  profesor?: string;
  hora?: string | null;
  periodo?: string;
  aula?: string;
  modalidad?: Modalidad | null;
  aperturaInscripcion?: string | null;
  cierreInscripcion?: string | null;
}

export interface FiltrosMesas {
  materiaId?: number;
  fecha?: string;
  estado?: EstadoMesa;
  periodo?: string;
}

export async function listarMesasExamen(filtros: FiltrosMesas = {}): Promise<MesaExamenResponse[]> {
  const params: Record<string, string | number> = {};
  if (filtros.materiaId) params.materiaId = filtros.materiaId;
  if (filtros.fecha) params.fecha = filtros.fecha;
  if (filtros.estado) params.estado = filtros.estado;
  if (filtros.periodo) params.periodo = filtros.periodo;
  const { data } = await httpClient.get<MesaExamenResponse[]>('/mesas-examen', { params });
  return data;
}

export async function obtenerMesaExamen(id: number): Promise<MesaExamenResponse> {
  const { data } = await httpClient.get<MesaExamenResponse>(`/mesas-examen/${id}`);
  return data;
}

export async function crearMesaExamen(request: MesaExamenRequest): Promise<MesaExamenResponse> {
  const { data } = await httpClient.post<MesaExamenResponse>('/mesas-examen', request);
  return data;
}

export async function actualizarMesaExamen(id: number, request: MesaExamenRequest): Promise<MesaExamenResponse> {
  const { data } = await httpClient.put<MesaExamenResponse>(`/mesas-examen/${id}`, request);
  return data;
}

export async function publicarMesaExamen(id: number): Promise<MesaExamenResponse> {
  const { data } = await httpClient.patch<MesaExamenResponse>(`/mesas-examen/${id}/publicar`);
  return data;
}

export async function cerrarMesaExamen(id: number): Promise<MesaExamenResponse> {
  const { data } = await httpClient.patch<MesaExamenResponse>(`/mesas-examen/${id}/cerrar`);
  return data;
}

export async function cancelarMesaExamen(id: number): Promise<MesaExamenResponse> {
  const { data } = await httpClient.patch<MesaExamenResponse>(`/mesas-examen/${id}/cancelar`);
  return data;
}

export async function eliminarMesaExamen(id: number): Promise<void> {
  await httpClient.delete(`/mesas-examen/${id}`);
}
