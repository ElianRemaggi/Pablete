import { httpClient } from './httpClient';
import type { LoginResponse } from '../types/auth';

export async function login(dni: string, password: string): Promise<LoginResponse> {
  const { data } = await httpClient.post<LoginResponse>('/auth/login', { dni, password });
  return data;
}

export async function logout(refreshToken: string): Promise<void> {
  await httpClient.post('/auth/logout', { refreshToken });
}

export interface RegistroRequest {
  dni: string;
  email: string;
  nombre: string;
  apellido: string;
  password: string;
}

/** Autoregistro publico de alumnos: el backend solo crea cuentas con rol ALUMNO. */
export async function registrar(request: RegistroRequest): Promise<void> {
  await httpClient.post('/auth/registro', request);
}
