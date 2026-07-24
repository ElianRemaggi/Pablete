import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, Stack, TextField, Typography } from '@mui/material';
import { useSnackbar } from '../../components/SnackbarProvider';
import { actualizarConfiguracion, obtenerConfiguracion, type ConfiguracionResponse } from '../../api/configuracionApi';

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
    </Stack>
  );
}
