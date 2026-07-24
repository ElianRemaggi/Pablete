import { httpClient } from './httpClient';

export interface InscripcionFinalResponse {
  id: number;
  materiaNombre: string;
  fechaMesa: string;
  turno: string;
  estado: string;
}

export async function listarInscripcionesFinalPropias(): Promise<InscripcionFinalResponse[]> {
  const { data } = await httpClient.get<InscripcionFinalResponse[]>('/inscripciones-final/me');
  return data;
}

export async function listarInscripcionesFinalPorAlumno(alumnoId: number): Promise<InscripcionFinalResponse[]> {
  const { data } = await httpClient.get<InscripcionFinalResponse[]>(`/inscripciones-final/alumno/${alumnoId}`);
  return data;
}

export async function inscribirFinal(mesaId: number): Promise<InscripcionFinalResponse> {
  const { data } = await httpClient.post<InscripcionFinalResponse>('/inscripciones-final', { mesaId });
  return data;
}

export async function modificarInscripcionFinal(id: number, mesaId: number): Promise<InscripcionFinalResponse> {
  const { data } = await httpClient.put<InscripcionFinalResponse>(`/inscripciones-final/${id}`, { mesaId });
  return data;
}

export async function darDeBajaInscripcionFinal(id: number): Promise<void> {
  await httpClient.delete(`/inscripciones-final/${id}`);
}
