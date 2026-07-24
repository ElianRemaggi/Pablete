import { httpClient } from './httpClient';

export interface InscripcionCursadaResponse {
  id: number;
  materiaNombre: string;
  comisionNombre: string;
  estado: string;
  fechaInscripcion: string;
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
