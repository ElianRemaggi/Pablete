import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, CardContent, CircularProgress, Grid, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { obtenerHistorialPropio } from '../../api/alumnosApi';
import { obtenerConfiguracion } from '../../api/configuracionApi';
import { obtenerCalendarioPropio } from '../../api/calendarioApi';
import { CalendarioConLeyenda } from '../../components/calendario/CalendarioConLeyenda';
import { useMesCalendario } from '../../hooks/useMesCalendario';

function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'medium', timeStyle: 'short' });
}

export function AlumnoDashboardPage() {
  const historialQuery = useQuery({ queryKey: ['alumno', 'historial'], queryFn: obtenerHistorialPropio });
  const configQuery = useQuery({ queryKey: ['configuracion'], queryFn: obtenerConfiguracion });
  const { mes, desde, hasta, cambiarMes } = useMesCalendario();
  const calendarioQuery = useQuery({
    queryKey: ['calendario', 'propio', desde.toISOString(), hasta.toISOString()],
    queryFn: () => obtenerCalendarioPropio(desde, hasta),
  });

  const resumen = { aprobadas: 0, regulares: 0, cursando: 0, libres: 0 };
  historialQuery.data?.forEach((item) => {
    if (item.estado === 'APROBADA') resumen.aprobadas += 1;
    else if (item.estado === 'REGULAR') resumen.regulares += 1;
    else if (item.estado === 'CURSANDO') resumen.cursando += 1;
    else if (item.estado === 'LIBRE') resumen.libres += 1;
  });

  const tarjetas: Array<[string, number]> = [
    ['Aprobadas', resumen.aprobadas],
    ['Regulares', resumen.regulares],
    ['Cursando', resumen.cursando],
    ['Libres', resumen.libres],
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Mi resumen académico</Typography>

      {historialQuery.isLoading && <CircularProgress />}
      {historialQuery.isError && <Alert severity="error">No se pudo cargar el historial académico.</Alert>}

      {historialQuery.data && (
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

      <Typography variant="h5">Fechas clave</Typography>
      {configQuery.isLoading && <CircularProgress />}
      {configQuery.isError && <Alert severity="error">No se pudo cargar la configuración institucional.</Alert>}
      {configQuery.data && (
        <Card>
          <CardContent>
            <Typography>
              Cierre de inscripción a cursadas: {formatearFecha(configQuery.data.fechaCierreCursadas)}
            </Typography>
            <Typography>
              Cierre de inscripción a finales: {formatearFecha(configQuery.data.fechaCierreFinales)}
            </Typography>
          </CardContent>
        </Card>
      )}

      <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
        <Button component={Link} to="/alumno/inscripcion-cursada" variant="contained">
          Inscribirme a cursada
        </Button>
        <Button component={Link} to="/alumno/inscripcion-final" variant="contained">
          Inscribirme a final
        </Button>
        <Button component={Link} to="/alumno/certificados" variant="outlined">
          Descargar certificado
        </Button>
      </Stack>

      <Typography variant="h5">Mi calendario</Typography>
      <CalendarioConLeyenda
        mes={mes}
        eventos={calendarioQuery.data}
        isLoading={calendarioQuery.isLoading}
        isError={calendarioQuery.isError}
        onCambiarMes={cambiarMes}
      />
    </Stack>
  );
}
