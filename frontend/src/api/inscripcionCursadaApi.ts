import { httpClient } from './httpClient';
import type { HorarioResponse } from './comisionesApi';

export interface InscripcionCursadaResponse {
  id: number;
  materiaNombre: string;
  comisionNombre: string;
  estado: string;
  fechaInscripcion: string;
  profesor: string | null;
  carreraNombre: string | null;
  horarios: HorarioResponse[];
  materiaId: number;
  comisionId: number;
  periodo: string;
  alumnoId: number;
  alumnoDni: string;
  alumnoNombre: string;
  alumnoApellido: string;
  alumnoLegajo: string;
  notaCursada: number | null;
  resultadoCursada: ResultadoCursada;
  fechaCalificacion: string | null;
}

export type ResultadoCursada = 'PENDIENTE' | 'APROBADA' | 'DESAPROBADA';



export async function listarInscriptosPorComision(comisionId: number): Promise<InscripcionCursadaResponse[]> {
  const { data } = await httpClient.get<InscripcionCursadaResponse[]>(`/inscripciones-cursada/comision/${comisionId}`);
  return data;
}

export async function calificarCursada(
  id: number,
  resultado: ResultadoCursada,
  nota: number | null,
): Promise<InscripcionCursadaResponse> {
  const { data } = await httpClient.put<InscripcionCursadaResponse>(`/inscripciones-cursada/${id}/calificacion`, {
    resultado,
    nota,
  });
  return data;
}

export async function listarInscripcionesCursadaPropias(): Promise<InscripcionCursadaResponse[]> {
  const { data } = await httpClient.get<InscripcionCursadaResponse[]>('/inscripciones-cursada/me');
  return data;
}

export async function listarInscripcionesCursadaPorAlumno(alumnoId: number): Promise<InscripcionCursadaResponse[]> {
  const { data } = await httpClient.get<InscripcionCursadaResponse[]>(`/inscripciones-cursada/alumno/${alumnoId}`);
  return data;
}

export async function inscribirCursada(comisionId: number): Promise<InscripcionCursadaResponse> {
  const { data } = await httpClient.post<InscripcionCursadaResponse>('/inscripciones-cursada', { comisionId });
  return data;
}

export async function modificarInscripcionCursada(
  id: number,
  comisionId: number,
): Promise<InscripcionCursadaResponse> {
  const { data } = await httpClient.put<InscripcionCursadaResponse>(`/inscripciones-cursada/${id}`, { comisionId });
  return data;
}

export async function darDeBajaInscripcionCursada(id: number): Promise<void> {
  await httpClient.delete(`/inscripciones-cursada/${id}`);
}
