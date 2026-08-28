import { Alert, Box, CircularProgress, Stack } from '@mui/material';
import { CalendarioMensual } from './CalendarioMensual';
import { LeyendaCalendario } from './LeyendaCalendario';
import type { EventoCalendarioResponse } from '../../api/calendarioApi';

interface CalendarioConLeyendaProps {
  mes: Date;
  eventos: EventoCalendarioResponse[] | undefined;
  isLoading: boolean;
  isError: boolean;
  onCambiarMes: (delta: number) => void;
}

export function CalendarioConLeyenda({ mes, eventos, isLoading, isError, onCambiarMes }: CalendarioConLeyendaProps) {
  if (isError) {
    return <Alert severity="error">No se pudo cargar el calendario.</Alert>;
  }

  return (
    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Box sx={{ flexGrow: 1, width: '100%', position: 'relative', minHeight: 200 }}>
        <CalendarioMensual mes={mes} eventos={eventos ?? []} onCambiarMes={onCambiarMes} />
        {isLoading && (
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'rgba(255,255,255,0.5)',
              borderRadius: 1,
            }}
          >
            <CircularProgress size={28} />
          </Box>
        )}
      </Box>
      <Box sx={{ minWidth: { md: 160 } }}>
        <LeyendaCalendario />
      </Box>
    </Stack>
  );
}
