import { httpClient } from './httpClient';

export interface HorarioResponse {
  id: number;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  aula: string;
}

export interface HorarioRequest {
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  aula: string;
}

export interface ComisionResponse {
  id: number;
  materiaId: number;
  materiaNombre: string;
  nombre: string;
  periodoLectivo: string;
  cupoMaximo: number;
  profesor: string | null;
  horarios: HorarioResponse[];
}

export interface ComisionRequest {
  materiaId: number;
  nombre: string;
  periodoLectivo: string;
  cupoMaximo: number;
  profesor?: string;
  horarios: HorarioRequest[];
}

export async function listarComisiones(): Promise<ComisionResponse[]> {
  const { data } = await httpClient.get<ComisionResponse[]>('/comisiones');
  return data;
}

export async function crearComision(request: ComisionRequest): Promise<ComisionResponse> {
  const { data } = await httpClient.post<ComisionResponse>('/comisiones', request);
  return data;
}

export async function actualizarComision(id: number, request: ComisionRequest): Promise<ComisionResponse> {
  const { data } = await httpClient.put<ComisionResponse>(`/comisiones/${id}`, request);
  return data;
}

export async function eliminarComision(id: number): Promise<void> {
  await httpClient.delete(`/comisiones/${id}`);
}

export async function agregarHorario(comisionId: number, request: HorarioRequest): Promise<ComisionResponse> {
  const { data } = await httpClient.post<ComisionResponse>(`/comisiones/${comisionId}/horarios`, request);
  return data;
}

export async function quitarHorario(comisionId: number, horarioId: number): Promise<ComisionResponse> {
  const { data } = await httpClient.delete<ComisionResponse>(`/comisiones/${comisionId}/horarios/${horarioId}`);
  return data;
}

interface InscripcionCursadaResponse {
  estado: string;
}

export async function contarInscriptosActivos(comisionId: number): Promise<number> {
  const { data } = await httpClient.get<InscripcionCursadaResponse[]>(`/inscripciones-cursada/comision/${comisionId}`);
  return data.filter((i) => i.estado !== 'BAJA').length;
}
