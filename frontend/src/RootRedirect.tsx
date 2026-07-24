import { Navigate } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import type { Rol } from './types/auth';

const RUTA_POR_ROL: Record<Rol, string> = {
  ALUMNO: '/alumno/dashboard',
  PRECEPTOR: '/preceptor/dashboard',
  ADMINISTRADOR: '/administrador/dashboard',
};

export function RootRedirect() {
  const { auth } = useAuth();
  if (!auth) {
    return <Navigate to="/login" replace />;
  }
  return <Navigate to={RUTA_POR_ROL[auth.roles[0]] ?? '/login'} replace />;
}
