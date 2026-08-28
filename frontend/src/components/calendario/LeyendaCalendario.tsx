import { Box, Stack, Typography } from '@mui/material';
import { COLORES_TIPO_EVENTO, ETIQUETAS_TIPO_EVENTO } from './coloresCalendario';
import type { TipoEventoCalendario } from '../../api/calendarioApi';

const TIPOS: TipoEventoCalendario[] = ['CURSADA', 'FINAL', 'INSCRIPCION', 'EVENTO_ACADEMICO', 'FECHA_LIMITE'];

export function LeyendaCalendario() {
  return (
    <Stack spacing={1}>
      <Typography variant="subtitle2" color="text.secondary">
        Referencias
      </Typography>
      <Stack spacing={0.75}>
        {TIPOS.map((tipo) => (
          <Stack key={tipo} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Box
              sx={{
                width: 12,
                height: 12,
                borderRadius: '3px',
                bgcolor: COLORES_TIPO_EVENTO[tipo],
                flexShrink: 0,
              }}
            />
            <Typography variant="body2">{ETIQUETAS_TIPO_EVENTO[tipo]}</Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}
