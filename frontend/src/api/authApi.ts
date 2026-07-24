import { httpClient } from './httpClient';
import type { LoginResponse } from '../types/auth';

export async function login(dni: string, password: string): Promise<LoginResponse> {
  const { data } = await httpClient.post<LoginResponse>('/auth/login', { dni, password });
  return data;
}

export async function logout(refreshToken: string): Promise<void> {
  await httpClient.post('/auth/logout', { refreshToken });
}
