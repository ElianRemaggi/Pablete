import { Alert, Button, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const INICIO_POR_ROL = {
  ADMINISTRADOR: '/administrador/dashboard',
  PRECEPTOR: '/preceptor/dashboard',
  ALUMNO: '/alumno/dashboard',
} as const;

/** Pantalla informativa para las rutas de funcionalidades fuera del alcance de la primera entrega. */
export function FuncionalidadNoDisponiblePage({ titulo }: { titulo: string }) {
  const { auth } = useAuth();
  const inicio = auth?.roles[0] ? INICIO_POR_ROL[auth.roles[0]] : '/login';
  return (
    <Stack spacing={3}>
      <Typography variant="h4">{titulo}</Typography>
      <Alert severity="info">
        Esta funcionalidad no está disponible en esta versión de SIGA. Por ahora el sistema cubre la gestión e
        inscripción a exámenes finales; las cursadas anuales se habilitarán en una próxima etapa.
      </Alert>
      <Button component={Link} to={inicio} variant="contained" sx={{ alignSelf: 'flex-start' }}>
        Volver al inicio
      </Button>
    </Stack>
  );
}
