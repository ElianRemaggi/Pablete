import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, CardContent, CircularProgress, Stack, TextField, Typography } from '@mui/material';
import { obtenerAlumnoPropio } from '../../api/alumnosApi';
import { actualizarPerfilPropio } from '../../api/usuariosApi';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { formatearFecha } from '../../utils/fechas';

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function AlumnoPerfilPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['alumno', 'propio'], queryFn: obtenerAlumnoPropio });
  const [email, setEmail] = useState('');

  useEffect(() => {
    if (data) setEmail(data.email);
  }, [data]);

  const guardarMutation = useMutation({
    mutationFn: actualizarPerfilPropio,
    onSuccess: () => {
      notify('Datos de contacto actualizados', 'success');
      queryClient.invalidateQueries({ queryKey: ['alumno', 'propio'] });
    },
    onError: (err) => notify(extraerMensajeError(err, 'No se pudo actualizar el email'), 'error'),
  });

  const emailInvalido = email !== '' && !EMAIL_VALIDO.test(email.trim());
  const sinCambios = !data || email.trim() === data.email;

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Datos personales</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">{extraerMensajeError(error, 'No se pudo cargar tu información de alumno.')}</Alert>}
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
                <strong>Fecha de ingreso:</strong> {formatearFecha(data.fechaIngreso)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                El nombre, el DNI y el legajo son datos institucionales: si hay un error, pedí la corrección en
                secretaría.
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      )}
      {data && (
        <Card>
          <CardContent>
            <Stack spacing={2} sx={{ maxWidth: 420 }}>
              <Typography variant="h6">Contacto</Typography>
              <TextField
                label="Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                error={emailInvalido}
                helperText={emailInvalido ? 'Email inválido' : undefined}
              />
              <Button
                variant="contained"
                sx={{ alignSelf: 'flex-start' }}
                disabled={sinCambios || emailInvalido || email.trim() === '' || guardarMutation.isPending}
                onClick={() => guardarMutation.mutate(email.trim())}
              >
                Guardar email
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
