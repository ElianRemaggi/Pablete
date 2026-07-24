import { httpClient } from './httpClient';

export type Turno = 'MANANA' | 'TARDE' | 'NOCHE';

export interface MesaExamenResponse {
  id: number;
  materiaId: number;
  materiaNombre: string;
  fecha: string;
  turno: string;
  cupoMaximo: number;
}

export interface MesaExamenRequest {
  materiaId: number;
  fecha: string;
  turno: Turno;
  cupoMaximo: number;
}

export async function listarMesasExamen(): Promise<MesaExamenResponse[]> {
  const { data } = await httpClient.get<MesaExamenResponse[]>('/mesas-examen');
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

export async function eliminarMesaExamen(id: number): Promise<void> {
  await httpClient.delete(`/mesas-examen/${id}`);
}
