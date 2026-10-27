import { httpClient } from './httpClient';

export type EstadoInscripcion = 'INSCRIPTO' | 'MODIFICADA' | 'BAJA';

export interface InscripcionFinalResponse {
  id: number;
  mesaId: number;
  materiaNombre: string;
  fechaMesa: string;
  turno: string;
  estado: EstadoInscripcion;
  profesor: string | null;
  carreraNombre: string | null;
  resultado: string | null;
  hora: string | null;
  periodo: string | null;
  aula: string | null;
  modalidad: string | null;
  mesaEstado: string;
  fechaInscripcion: string;
  alumnoId: number;
  alumnoDni: string;
  alumnoNombre: string;
  alumnoApellido: string;
  alumnoLegajo: string;
  nota: number | null;
}

export type ResultadoFinal = 'AUSENTE' | 'DESAPROBADO' | 'APROBADO';



export async function registrarResultadoFinal(
  id: number,
  resultado: ResultadoFinal,
  nota: number | null,
): Promise<InscripcionFinalResponse> {
  const { data } = await httpClient.put<InscripcionFinalResponse>(`/inscripciones-final/${id}/resultado`, {
    resultado,
    nota,
  });
  return data;
}

export async function listarInscripcionesFinalPropias(): Promise<InscripcionFinalResponse[]> {
  const { data } = await httpClient.get<InscripcionFinalResponse[]>('/inscripciones-final/me');
  return data;
}

export async function listarInscripcionesFinalPorAlumno(alumnoId: number): Promise<InscripcionFinalResponse[]> {
  const { data } = await httpClient.get<InscripcionFinalResponse[]>(`/inscripciones-final/alumno/${alumnoId}`);
  return data;
}

export async function listarInscriptosPorMesa(mesaId: number): Promise<InscripcionFinalResponse[]> {
  const { data } = await httpClient.get<InscripcionFinalResponse[]>(`/inscripciones-final/mesa/${mesaId}`);
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
