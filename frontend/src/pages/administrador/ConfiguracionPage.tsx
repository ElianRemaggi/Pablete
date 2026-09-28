import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, CardContent, CircularProgress, Stack, TextField, Typography } from '@mui/material';
import { useSnackbar } from '../../components/SnackbarProvider';
import { actualizarConfiguracion, obtenerConfiguracion, type ConfiguracionResponse } from '../../api/configuracionApi';
import { recargarHistorialDesdeExcel } from '../../api/historialAcademicoApi';

function toDatetimeLocal(iso: string): string {
  return iso.slice(0, 16);
}

export function ConfiguracionPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const { data, isLoading, isError } = useQuery({ queryKey: ['configuracion'], queryFn: obtenerConfiguracion });

  const { register, handleSubmit, reset } = useForm<ConfiguracionResponse>();

  useEffect(() => {
    if (data) {
      reset({
        ...data,
        fechaAperturaCursadas: toDatetimeLocal(data.fechaAperturaCursadas),
        fechaCierreCursadas: toDatetimeLocal(data.fechaCierreCursadas),
        fechaAperturaFinales: toDatetimeLocal(data.fechaAperturaFinales),
        fechaCierreFinales: toDatetimeLocal(data.fechaCierreFinales),
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: actualizarConfiguracion,
    onSuccess: () => {
      notify('Configuración actualizada correctamente', 'success');
      queryClient.invalidateQueries({ queryKey: ['configuracion'] });
    },
    onError: () => notify('No se pudo actualizar la configuración (revisá las fechas)', 'error'),
  });

  const recargaHistorial = useMutation({
    mutationFn: recargarHistorialDesdeExcel,
    onSuccess: (resultado) => {
      notify(
        `Historial académico sincronizado: ${resultado.actualizados} actualizados, ` +
          `${resultado.alumnosNoEncontrados} alumno(s) no encontrado(s), ` +
          `${resultado.materiasNoEncontradas} materia(s) no encontrada(s).`,
        resultado.alumnosNoEncontrados === 0 && resultado.materiasNoEncontradas === 0 ? 'success' : 'warning',
      );
    },
    onError: () => notify('No se pudo sincronizar el historial académico desde el Excel', 'error'),
  });

  return (
    <Stack spacing={3} sx={{ maxWidth: 500 }}>
      <Typography variant="h4">Configuración institucional</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar la configuración.</Alert>}
      {data && (
        <Stack component="form" spacing={2} onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <TextField
            label="Apertura inscripción a cursadas"
            type="datetime-local"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('fechaAperturaCursadas')}
          />
          <TextField
            label="Cierre inscripción a cursadas"
            type="datetime-local"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('fechaCierreCursadas')}
          />
          <TextField
            label="Apertura inscripción a finales"
            type="datetime-local"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('fechaAperturaFinales')}
          />
          <TextField
            label="Cierre inscripción a finales"
            type="datetime-local"
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('fechaCierreFinales')}
          />
          <TextField
            label="Límite de intentos por final"
            type="number"
            {...register('limiteIntentosFinal', { valueAsNumber: true })}
          />
          <TextField
            label="Horas de bloqueo previo al final"
            type="number"
            {...register('horasBloqueoFinal', { valueAsNumber: true })}
          />
          <Button type="submit" variant="contained" disabled={mutation.isPending}>
            Guardar
          </Button>
        </Stack>
      )}

      <Card>
        <CardContent>
          <Stack spacing={1.5}>
            <Typography variant="h6">Historial académico (Excel)</Typography>
            <Typography variant="body2" color="text.secondary">
              Relee el archivo .xlsx configurado en el servidor y vuelca sus cambios a la base. Usalo si alguien
              editó el archivo por fuera del sistema.
            </Typography>
            <Button
              variant="outlined"
              onClick={() => recargaHistorial.mutate()}
              disabled={recargaHistorial.isPending}
              sx={{ alignSelf: 'flex-start' }}
            >
              {recargaHistorial.isPending ? 'Sincronizando…' : 'Recargar desde Excel'}
            </Button>
            {recargaHistorial.data && recargaHistorial.data.advertencias.length > 0 && (
              <Alert severity="warning" sx={{ whiteSpace: 'pre-line' }}>
                {recargaHistorial.data.advertencias.slice(0, 10).join('\n')}
                {recargaHistorial.data.advertencias.length > 10 ? '\n…' : ''}
              </Alert>
            )}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
