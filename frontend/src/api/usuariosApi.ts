import { httpClient } from './httpClient';

export interface UsuarioResponse {
  id: number;
  dni: string;
  email: string;
  nombre: string;
  apellido: string;
  activo: boolean;
  roles: string[];
  fechaCreacion: string;
}

export interface UsuarioRequest {
  dni: string;
  email: string;
  nombre: string;
  apellido: string;
  password: string;
  roles: string[];
}

export interface UsuarioUpdateRequest {
  email?: string;
  nombre?: string;
  apellido?: string;
}

interface PageResponse<T> {
  content: T[];
}

export async function listarUsuarios(): Promise<UsuarioResponse[]> {
  const { data } = await httpClient.get<PageResponse<UsuarioResponse>>('/usuarios', { params: { size: 200 } });
  return data.content;
}

export async function crearUsuario(request: UsuarioRequest): Promise<UsuarioResponse> {
  const { data } = await httpClient.post<UsuarioResponse>('/usuarios', request);
  return data;
}

export async function actualizarUsuario(id: number, request: UsuarioUpdateRequest): Promise<UsuarioResponse> {
  const { data } = await httpClient.put<UsuarioResponse>(`/usuarios/${id}`, request);
  return data;
}

export async function activarUsuario(id: number): Promise<UsuarioResponse> {
  const { data } = await httpClient.patch<UsuarioResponse>(`/usuarios/${id}/activar`);
  return data;
}

export async function desactivarUsuario(id: number): Promise<UsuarioResponse> {
  const { data } = await httpClient.patch<UsuarioResponse>(`/usuarios/${id}/desactivar`);
  return data;
}

export async function actualizarRolesUsuario(id: number, roles: string[]): Promise<UsuarioResponse> {
  const { data } = await httpClient.put<UsuarioResponse>(`/usuarios/${id}/roles`, { roles });
  return data;
}

/** El propio usuario solo puede cambiar su email (DNI, nombre y roles los gestiona un administrador). */
export async function actualizarPerfilPropio(email: string): Promise<UsuarioResponse> {
  const { data } = await httpClient.put<UsuarioResponse>('/usuarios/me', { email });
  return data;
}
