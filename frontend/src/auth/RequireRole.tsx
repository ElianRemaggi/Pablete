import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import type { Rol } from '../types/auth';

interface RequireRoleProps {
  rol: Rol;
}

export function RequireRole({ rol }: RequireRoleProps) {
  const { auth } = useAuth();

  if (!auth) {
    return <Navigate to="/login" replace />;
  }
  if (!auth.roles.includes(rol)) {
    return <Navigate to="/403" replace />;
  }
  return <Outlet />;
}
