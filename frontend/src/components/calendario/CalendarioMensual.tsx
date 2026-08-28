import { useMemo, useState, type MouseEvent } from 'react';
import { Box, IconButton, Paper, Popover, Stack, Typography, useTheme } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { COLORES_TIPO_EVENTO } from './coloresCalendario';
import type { EventoCalendarioResponse } from '../../api/calendarioApi';

const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const MAX_EVENTOS_VISIBLES = 3;

interface CalendarioMensualProps {
  mes: Date;
  eventos: EventoCalendarioResponse[];
  onCambiarMes: (delta: number) => void;
}

function claveFecha(fecha: Date): string {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}

function claveFechaIso(iso: string): string {
  return iso.slice(0, 10);
}

function construirDias(mes: Date): Date[] {
  const primerDiaMes = new Date(mes.getFullYear(), mes.getMonth(), 1);
  const offset = (primerDiaMes.getDay() + 6) % 7; // lunes = 0
  const primerCelda = new Date(primerDiaMes);
  primerCelda.setDate(primerCelda.getDate() - offset);
  return Array.from({ length: 42 }, (_, i) => {
    const dia = new Date(primerCelda);
    dia.setDate(dia.getDate() + i);
    return dia;
  });
}

export function CalendarioMensual({ mes, eventos, onCambiarMes }: CalendarioMensualProps) {
  const theme = useTheme();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [diaSeleccionado, setDiaSeleccionado] = useState<Date | null>(null);

  const dias = useMemo(() => construirDias(mes), [mes]);

  const eventosPorDia = useMemo(() => {
    const mapa = new Map<string, EventoCalendarioResponse[]>();
    for (const evento of eventos) {
      const clave = claveFechaIso(evento.inicio);
      const lista = mapa.get(clave) ?? [];
      lista.push(evento);
      mapa.set(clave, lista);
    }
    return mapa;
  }, [eventos]);

  const abrirDia = (event: MouseEvent<HTMLElement>, dia: Date) => {
    setAnchorEl(event.currentTarget);
    setDiaSeleccionado(dia);
  };

  const cerrarPopover = () => {
    setAnchorEl(null);
    setDiaSeleccionado(null);
  };

  const eventosDelDiaSeleccionado = diaSeleccionado ? (eventosPorDia.get(claveFecha(diaSeleccionado)) ?? []) : [];

  const nombreMes = mes.toLocaleDateString('es-AR', { month: 'long', year: 'numeric' });
  const hoy = claveFecha(new Date());

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <IconButton onClick={() => onCambiarMes(-1)} aria-label="Mes anterior" size="small">
          <ChevronLeftIcon />
        </IconButton>
        <Typography variant="subtitle1" sx={{ textTransform: 'capitalize' }}>
          {nombreMes}
        </Typography>
        <IconButton onClick={() => onCambiarMes(1)} aria-label="Mes siguiente" size="small">
          <ChevronRightIcon />
        </IconButton>
      </Stack>

      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 0.5 }}>
        {DIAS_SEMANA.map((dia) => (
          <Typography key={dia} variant="caption" color="text.secondary" align="center" sx={{ fontWeight: 600 }}>
            {dia}
          </Typography>
        ))}

        {dias.map((dia) => {
          const clave = claveFecha(dia);
          const eventosDia = eventosPorDia.get(clave) ?? [];
          const esDelMes = dia.getMonth() === mes.getMonth();
          const esHoy = clave === hoy;

          return (
            <Box
              key={clave}
              onClick={eventosDia.length > 0 ? (e) => abrirDia(e, dia) : undefined}
              sx={{
                minHeight: 76,
                p: 0.5,
                borderRadius: 1,
                border: '1px solid',
                borderColor: esHoy ? theme.palette.primary.main : 'divider',
                bgcolor: esDelMes ? 'background.paper' : 'action.hover',
                cursor: eventosDia.length > 0 ? 'pointer' : 'default',
                opacity: esDelMes ? 1 : 0.5,
                display: 'flex',
                flexDirection: 'column',
                gap: 0.25,
              }}
            >
              <Typography variant="caption" sx={{ fontWeight: esHoy ? 700 : 400 }}>
                {dia.getDate()}
              </Typography>
              {eventosDia.slice(0, MAX_EVENTOS_VISIBLES).map((evento) => (
                <Box
                  key={evento.id}
                  sx={{
                    fontSize: '0.65rem',
                    lineHeight: 1.3,
                    px: 0.5,
                    py: 0.1,
                    borderRadius: '4px',
                    color: '#fff',
                    bgcolor: COLORES_TIPO_EVENTO[evento.tipo],
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                  title={evento.titulo}
                >
                  {evento.titulo}
                </Box>
              ))}
              {eventosDia.length > MAX_EVENTOS_VISIBLES && (
                <Typography variant="caption" color="text.secondary">
                  +{eventosDia.length - MAX_EVENTOS_VISIBLES} más
                </Typography>
              )}
            </Box>
          );
        })}
      </Box>

      <Popover
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={cerrarPopover}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <Stack spacing={1} sx={{ p: 2, maxWidth: 320 }}>
          <Typography variant="subtitle2">
            {diaSeleccionado?.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })}
          </Typography>
          {eventosDelDiaSeleccionado.map((evento) => (
            <Stack key={evento.id} direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
              <Box
                sx={{
                  width: 10,
                  height: 10,
                  borderRadius: '3px',
                  bgcolor: COLORES_TIPO_EVENTO[evento.tipo],
                  mt: 0.5,
                  flexShrink: 0,
                }}
              />
              <Box>
                <Typography variant="body2">{evento.titulo}</Typography>
                {evento.detalle && (
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                    {evento.detalle}
                  </Typography>
                )}
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  {new Date(evento.inicio).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })}
                  {evento.ubicacion ? ` · ${evento.ubicacion}` : ''}
                </Typography>
              </Box>
            </Stack>
          ))}
        </Stack>
      </Popover>
    </Paper>
  );
}
