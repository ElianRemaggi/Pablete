import { httpClient } from './httpClient';

export type TipoRequisito = 'CURSAR' | 'APROBAR';

export interface CorrelatividadResponse {
  id: number;
  materiaId: number;
  materiaNombre: string;
  materiaRequeridaId: number;
  materiaRequeridaNombre: string;
  tipoRequisito: string;
}

export interface CorrelatividadRequest {
  materiaId: number;
  materiaRequeridaId: number;
  tipoRequisito: TipoRequisito;
}

export async function listarCorrelatividadesPorMateria(materiaId: number): Promise<CorrelatividadResponse[]> {
  const { data } = await httpClient.get<CorrelatividadResponse[]>(`/correlatividades/materia/${materiaId}`);
  return data;
}

export async function crearCorrelatividad(request: CorrelatividadRequest): Promise<CorrelatividadResponse> {
  const { data } = await httpClient.post<CorrelatividadResponse>('/correlatividades', request);
  return data;
}

export async function eliminarCorrelatividad(id: number): Promise<void> {
  await httpClient.delete(`/correlatividades/${id}`);
}
