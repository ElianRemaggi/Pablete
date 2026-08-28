import { useQuery } from '@tanstack/react-query';
import { Stack, Typography } from '@mui/material';
import { CalendarioConLeyenda } from '../../components/calendario/CalendarioConLeyenda';
import { obtenerCalendarioPropio } from '../../api/calendarioApi';
import { useMesCalendario } from '../../hooks/useMesCalendario';

export function AlumnoCalendarioPage() {
  const { mes, desde, hasta, cambiarMes } = useMesCalendario();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['calendario', 'propio', desde.toISOString(), hasta.toISOString()],
    queryFn: () => obtenerCalendarioPropio(desde, hasta),
  });

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Mi calendario</Typography>
      <CalendarioConLeyenda mes={mes} eventos={data} isLoading={isLoading} isError={isError} onCambiarMes={cambiarMes} />
    </Stack>
  );
}
