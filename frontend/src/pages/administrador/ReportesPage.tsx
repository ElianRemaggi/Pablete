import { useQuery } from '@tanstack/react-query';
import { Alert, CircularProgress, Stack, Typography, useTheme } from '@mui/material';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { listarComisiones, contarInscriptosActivos } from '../../api/comisionesApi';
import { obtenerTasaAprobacionPorMateria } from '../../api/reportesApi';

async function cargarOcupacion() {
  const comisiones = await listarComisiones();
  const conteos = await Promise.all(comisiones.map((c) => contarInscriptosActivos(c.id)));
  return comisiones.map((c, i) => ({
    nombre: `${c.materiaNombre} (${c.nombre})`,
    inscriptos: conteos[i],
    cupoMaximo: c.cupoMaximo,
  }));
}

async function cargarTasaAprobacion() {
  const filas = await obtenerTasaAprobacionPorMateria();
  return filas.map((f) => ({
    nombre: f.materiaNombre,
    tasaAprobacion: f.total === 0 ? 0 : Math.round((f.aprobadas / f.total) * 100),
  }));
}

export function ReportesPage() {
  const theme = useTheme();
  const ocupacionQuery = useQuery({ queryKey: ['reportes', 'ocupacion'], queryFn: cargarOcupacion });
  const aprobacionQuery = useQuery({ queryKey: ['reportes', 'aprobacion'], queryFn: cargarTasaAprobacion });

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Reportes</Typography>

      <Typography variant="h6">Ocupación de cupos por comisión</Typography>
      {ocupacionQuery.isLoading && <CircularProgress />}
      {ocupacionQuery.isError && <Alert severity="error">No se pudo cargar el reporte de ocupación.</Alert>}
      {ocupacionQuery.data && ocupacionQuery.data.length === 0 && (
        <Alert severity="info">No hay comisiones cargadas todavía.</Alert>
      )}
      {ocupacionQuery.data && ocupacionQuery.data.length > 0 && (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={ocupacionQuery.data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="nombre" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" height={80} />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Legend />
            <Bar dataKey="cupoMaximo" name="Cupo máximo" fill={theme.palette.primary.main} />
            <Bar dataKey="inscriptos" name="Inscriptos" fill={theme.palette.secondary.main} />
          </BarChart>
        </ResponsiveContainer>
      )}

      <Typography variant="h6">Tasa de aprobación por materia</Typography>
      {aprobacionQuery.isLoading && <CircularProgress />}
      {aprobacionQuery.isError && <Alert severity="error">No se pudo cargar el reporte de aprobación.</Alert>}
      {aprobacionQuery.data && aprobacionQuery.data.length === 0 && (
        <Alert severity="info">Todavía no hay estados académicos registrados.</Alert>
      )}
      {aprobacionQuery.data && aprobacionQuery.data.length > 0 && (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={aprobacionQuery.data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="nombre" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" height={80} />
            <YAxis unit="%" domain={[0, 100]} />
            <Tooltip formatter={(value) => `${value}%`} />
            <Bar dataKey="tasaAprobacion" name="Tasa de aprobación" fill={theme.palette.primary.main} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </Stack>
  );
}
