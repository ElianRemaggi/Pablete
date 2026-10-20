import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Link, Paper, TextField, Typography } from '@mui/material';
import { useAuth } from '../auth/AuthContext';
import { login as loginRequest } from '../api/authApi';
import { extraerMensajeError } from '../utils/apiError';
import type { Rol } from '../types/auth';
import { COLOR_PRIMARIO, COLOR_PRIMARIO_OSCURO } from '../theme';
import logoIset from '../assets/logo-iset.jpg';

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
  const recienRegistrado = (useLocation().state as { registrado?: boolean } | null)?.registrado === true;
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
    } catch (err) {
      setError(extraerMensajeError(err, 'No se pudo iniciar sesión. Verificá tu conexión e intentá de nuevo.'));
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100svh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        overflow: 'hidden',
        p: 2,
        background: `linear-gradient(135deg, ${COLOR_PRIMARIO} 0%, ${COLOR_PRIMARIO_OSCURO} 100%)`,
      }}
    >
      <Box
        component="img"
        src={logoIset}
        alt=""
        aria-hidden="true"
        sx={{
          position: 'absolute',
          right: { xs: '50%', md: '8%' },
          top: '50%',
          transform: { xs: 'translate(50%, -50%)', md: 'translateY(-50%)' },
          width: { xs: 480, sm: 620, md: 720 },
          maxWidth: 'none',
          opacity: { xs: 0.14, md: 0.22 },
          mixBlendMode: 'multiply',
          pointerEvents: 'none',
          userSelect: 'none',
        }}
      />

      <Paper
        elevation={6}
        sx={{
          position: 'relative',
          zIndex: 1,
          p: 4,
          width: '100%',
          maxWidth: 380,
          borderRadius: 3,
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
          <Box component="img" src={logoIset} alt="Instituto Superior de Estudios Técnicos" sx={{ height: 72 }} />
        </Box>
        <Typography variant="h5" component="h1" align="center" gutterBottom>
          SIGA — Iniciar sesión
        </Typography>
        <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 1 }}>
          Sistema Web de Gestión Académica
        </Typography>
        {recienRegistrado && !error && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Cuenta creada. Ingresá con tu DNI y la contraseña que elegiste.
          </Alert>
        )}
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
        <Typography variant="body2" align="center" sx={{ mt: 2 }}>
          ¿Sos alumno y no tenés cuenta?{' '}
          <Link component={RouterLink} to="/registro">
            Registrate
          </Link>
        </Typography>
      </Paper>
    </Box>
  );
}
