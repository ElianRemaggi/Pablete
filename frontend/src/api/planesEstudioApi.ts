import { httpClient } from './httpClient';

export interface PlanEstudioResponse {
  id: number;
  carreraId: number;
  version: string;
  vigente: boolean;
}

export interface PlanEstudioRequest {
  version: string;
  vigente: boolean;
}

export async function listarPlanesDeCarrera(carreraId: number): Promise<PlanEstudioResponse[]> {
  const { data } = await httpClient.get<PlanEstudioResponse[]>(`/carreras/${carreraId}/planes`);
  return data;
}

export async function crearPlanEstudio(carreraId: number, request: PlanEstudioRequest): Promise<PlanEstudioResponse> {
  const { data } = await httpClient.post<PlanEstudioResponse>(`/carreras/${carreraId}/planes`, request);
  return data;
}

export async function actualizarPlanEstudio(id: number, request: PlanEstudioRequest): Promise<PlanEstudioResponse> {
  const { data } = await httpClient.put<PlanEstudioResponse>(`/planes-estudio/${id}`, request);
  return data;
}

export async function asociarMateriaAPlan(
  materiaId: number,
  planId: number,
  request: { anio: number; cuatrimestre: number },
): Promise<void> {
  await httpClient.post(`/materias/${materiaId}/plan/${planId}`, request);
}
