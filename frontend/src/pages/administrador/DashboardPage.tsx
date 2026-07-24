import { useQuery } from '@tanstack/react-query';
import { Alert, Card, CardContent, CircularProgress, Grid, Stack, Typography } from '@mui/material';
import { listarCarreras } from '../../api/carrerasApi';
import { listarMaterias } from '../../api/materiasApi';
import { listarUsuarios } from '../../api/usuariosApi';
import { listarComisiones } from '../../api/comisionesApi';

export function AdministradorDashboardPage() {
  const carrerasQuery = useQuery({ queryKey: ['carreras'], queryFn: listarCarreras });
  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });
  const usuariosQuery = useQuery({ queryKey: ['usuarios'], queryFn: listarUsuarios });
  const comisionesQuery = useQuery({ queryKey: ['comisiones'], queryFn: listarComisiones });

  const cargando =
    carrerasQuery.isLoading || materiasQuery.isLoading || usuariosQuery.isLoading || comisionesQuery.isLoading;
  const conError =
    carrerasQuery.isError || materiasQuery.isError || usuariosQuery.isError || comisionesQuery.isError;

  const tarjetas: Array<[string, number | undefined]> = [
    ['Carreras', carrerasQuery.data?.length],
    ['Materias', materiasQuery.data?.length],
    ['Usuarios', usuariosQuery.data?.length],
    ['Comisiones', comisionesQuery.data?.length],
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Resumen institucional</Typography>
      {cargando && <CircularProgress />}
      {conError && <Alert severity="error">No se pudieron cargar algunos indicadores.</Alert>}
      {!cargando && !conError && (
        <Grid container spacing={2}>
          {tarjetas.map(([label, valor]) => (
            <Grid key={label} size={{ xs: 6, sm: 3 }}>
              <Card>
                <CardContent>
                  <Typography variant="h3">{valor}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {label}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Stack>
  );
}
