export type Rol = 'ALUMNO' | 'PRECEPTOR' | 'ADMINISTRADOR';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  rol: Rol;
  nombre: string;
}

export interface JwtClaims {
  sub: string;
  dni: string;
  roles: Rol[];
  iat: number;
  exp: number;
}

export interface AuthState {
  accessToken: string;
  refreshToken: string;
  usuarioId: number;
  dni: string;
  roles: Rol[];
  nombre: string;
}
