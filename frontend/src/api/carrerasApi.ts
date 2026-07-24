import { httpClient } from './httpClient';

export interface CarreraResponse {
  id: number;
  codigo: string;
  nombre: string;
  activa: boolean;
}

export interface CarreraRequest {
  codigo: string;
  nombre: string;
}

export async function listarCarreras(): Promise<CarreraResponse[]> {
  const { data } = await httpClient.get<CarreraResponse[]>('/carreras');
  return data;
}

export async function crearCarrera(request: CarreraRequest): Promise<CarreraResponse> {
  const { data } = await httpClient.post<CarreraResponse>('/carreras', request);
  return data;
}

export async function actualizarCarrera(id: number, request: CarreraRequest): Promise<CarreraResponse> {
  const { data } = await httpClient.put<CarreraResponse>(`/carreras/${id}`, request);
  return data;
}

export async function darDeBajaCarrera(id: number): Promise<void> {
  await httpClient.delete(`/carreras/${id}`);
}
