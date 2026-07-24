import { createContext, useContext, useSyncExternalStore, type ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { getAuth, setAuth, subscribe } from './tokenStore';
import type { AuthState, JwtClaims, LoginResponse } from '../types/auth';

interface AuthContextValue {
  auth: AuthState | null;
  login: (response: LoginResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toAuthState(response: LoginResponse): AuthState {
  const claims = jwtDecode<JwtClaims>(response.accessToken);
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    usuarioId: Number(claims.sub),
    dni: claims.dni,
    roles: claims.roles,
    nombre: response.nombre,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const auth = useSyncExternalStore(subscribe, getAuth);

  const value: AuthContextValue = {
    auth,
    login: (response) => setAuth(toAuthState(response)),
    logout: () => setAuth(null),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}
