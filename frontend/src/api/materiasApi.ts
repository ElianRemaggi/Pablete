import { httpClient } from './httpClient';

export interface MateriaResponse {
  id: number;
  codigo: string;
  nombre: string;
  cargaHoraria: number;
  activa: boolean;
}

export interface MateriaRequest {
  codigo: string;
  nombre: string;
  cargaHoraria: number;
}

export async function listarMaterias(): Promise<MateriaResponse[]> {
  const { data } = await httpClient.get<MateriaResponse[]>('/materias');
  return data;
}

export async function listarMateriasPorPlan(planId: number): Promise<MateriaResponse[]> {
  const { data } = await httpClient.get<MateriaResponse[]>('/materias', { params: { planId } });
  return data;
}

export async function crearMateria(request: MateriaRequest): Promise<MateriaResponse> {
  const { data } = await httpClient.post<MateriaResponse>('/materias', request);
  return data;
}

export async function actualizarMateria(id: number, request: MateriaRequest): Promise<MateriaResponse> {
  const { data } = await httpClient.put<MateriaResponse>(`/materias/${id}`, request);
  return data;
}

export async function darDeBajaMateria(id: number): Promise<void> {
  await httpClient.delete(`/materias/${id}`);
}
