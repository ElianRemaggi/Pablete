import { useQuery } from '@tanstack/react-query';
import { Alert, Card, CardContent, CircularProgress, Stack, Typography } from '@mui/material';
import { obtenerAlumnoPropio } from '../../api/alumnosApi';

export function AlumnoPerfilPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ['alumno', 'propio'], queryFn: obtenerAlumnoPropio });

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Mi perfil</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar tu información de alumno.</Alert>}
      {data && (
        <Card>
          <CardContent>
            <Stack spacing={1}>
              <Typography>
                <strong>Nombre:</strong> {data.nombre} {data.apellido}
              </Typography>
              <Typography>
                <strong>DNI:</strong> {data.dni}
              </Typography>
              <Typography>
                <strong>Legajo:</strong> {data.legajo}
              </Typography>
              <Typography>
                <strong>Fecha de ingreso:</strong> {new Date(data.fechaIngreso).toLocaleDateString('es-AR')}
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
