import { useQuery } from '@tanstack/react-query';
import { Stack, Typography } from '@mui/material';
import { CalendarioConLeyenda } from '../../components/calendario/CalendarioConLeyenda';
import { obtenerCalendarioInstitucional } from '../../api/calendarioApi';
import { useMesCalendario } from '../../hooks/useMesCalendario';

export function PreceptorCalendarioPage() {
  const { mes, desde, hasta, cambiarMes } = useMesCalendario();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['calendario', 'institucional', desde.toISOString(), hasta.toISOString()],
    queryFn: () => obtenerCalendarioInstitucional(desde, hasta),
  });

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Calendario institucional</Typography>
      <CalendarioConLeyenda mes={mes} eventos={data} isLoading={isLoading} isError={isError} onCambiarMes={cambiarMes} />
    </Stack>
  );
}
