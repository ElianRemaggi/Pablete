import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Link, Paper, TextField, Typography } from '@mui/material';
import { registrar } from '../api/authApi';
import { extraerMensajeError } from '../utils/apiError';
import { COLOR_PRIMARIO, COLOR_PRIMARIO_OSCURO } from '../theme';
import logoIset from '../assets/logo-iset.jpg';

const schema = z
  .object({
    dni: z.string().regex(/^\d{7,9}$/, 'DNI inválido (7 a 9 dígitos, sin puntos)'),
    nombre: z.string().trim().min(1, 'El nombre es obligatorio'),
    apellido: z.string().trim().min(1, 'El apellido es obligatorio'),
    email: z.string().trim().email('Email inválido'),
    password: z
      .string()
      .regex(/^(?=.*[A-Z])(?=.*\d).{8,}$/, 'Al menos 8 caracteres, una mayúscula y un número'),
    confirmacion: z.string(),
  })
  .refine((v) => v.password === v.confirmacion, { path: ['confirmacion'], message: 'Las contraseñas no coinciden' });

type FormValues = z.infer<typeof schema>;

export function RegistroPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async ({ confirmacion: _confirmacion, ...datos }: FormValues) => {
    setError(null);
    try {
      await registrar(datos);
      navigate('/login', { replace: true, state: { registrado: true } });
    } catch (err) {
      setError(extraerMensajeError(err, 'No se pudo completar el registro. Reintentá en unos instantes.'));
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100svh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
        background: `linear-gradient(135deg, ${COLOR_PRIMARIO} 0%, ${COLOR_PRIMARIO_OSCURO} 100%)`,
      }}
    >
      <Paper elevation={6} sx={{ p: 4, width: '100%', maxWidth: 420, borderRadius: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
          <Box component="img" src={logoIset} alt="Instituto Superior de Estudios Técnicos" sx={{ height: 64 }} />
        </Box>
        <Typography variant="h5" component="h1" align="center" gutterBottom>
          SIGA — Registro de alumnos
        </Typography>
        <Typography variant="body2" align="center" color="text.secondary">
          Usá tu DNI real: es el que vincula tu cuenta con tu historial académico.
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField label="DNI" fullWidth margin="dense" autoFocus error={!!errors.dni} helperText={errors.dni?.message} {...register('dni')} />
          <TextField label="Nombre" fullWidth margin="dense" error={!!errors.nombre} helperText={errors.nombre?.message} {...register('nombre')} />
          <TextField label="Apellido" fullWidth margin="dense" error={!!errors.apellido} helperText={errors.apellido?.message} {...register('apellido')} />
          <TextField label="Email" type="email" fullWidth margin="dense" error={!!errors.email} helperText={errors.email?.message} {...register('email')} />
          <TextField label="Contraseña" type="password" fullWidth margin="dense" error={!!errors.password} helperText={errors.password?.message} {...register('password')} />
          <TextField label="Repetir contraseña" type="password" fullWidth margin="dense" error={!!errors.confirmacion} helperText={errors.confirmacion?.message} {...register('confirmacion')} />
          <Button type="submit" fullWidth variant="contained" sx={{ mt: 2 }} disabled={isSubmitting}>
            Crear cuenta
          </Button>
        </Box>
        <Typography variant="body2" align="center" sx={{ mt: 2 }}>
          ¿Ya tenés cuenta?{' '}
          <Link component={RouterLink} to="/login">
            Iniciar sesión
          </Link>
        </Typography>
      </Paper>
    </Box>
  );
}
