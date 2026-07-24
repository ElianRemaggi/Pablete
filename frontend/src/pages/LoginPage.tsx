import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material';
import { useAuth } from '../auth/AuthContext';
import { login as loginRequest } from '../api/authApi';
import type { Rol } from '../types/auth';

const schema = z.object({
  dni: z.string().regex(/^\d{7,9}$/, 'DNI inválido (7 a 9 dígitos)'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres'),
});

type FormValues = z.infer<typeof schema>;

const RUTA_POR_ROL: Record<Rol, string> = {
  ALUMNO: '/alumno/dashboard',
  PRECEPTOR: '/preceptor/dashboard',
  ADMINISTRADOR: '/administrador/dashboard',
};

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      const response = await loginRequest(values.dni, values.password);
      login(response);
      navigate(RUTA_POR_ROL[response.rol], { replace: true });
    } catch {
      setError('DNI o contraseña incorrectos');
    }
  };

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 12 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h5" component="h1" gutterBottom>
            SIGA — Iniciar sesión
          </Typography>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="DNI"
              fullWidth
              margin="normal"
              autoFocus
              error={!!errors.dni}
              helperText={errors.dni?.message}
              {...register('dni')}
            />
            <TextField
              label="Contraseña"
              type="password"
              fullWidth
              margin="normal"
              error={!!errors.password}
              helperText={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" fullWidth variant="contained" sx={{ mt: 3 }} disabled={isSubmitting}>
              Ingresar
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
}
