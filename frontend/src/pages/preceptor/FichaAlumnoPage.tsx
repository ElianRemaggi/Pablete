import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Alert, Card, CardContent, Chip, CircularProgress, Stack, Tab, Tabs, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { obtenerAlumnoPorId, obtenerHistorialPorId, type HistorialAcademicoResponse } from '../../api/alumnosApi';
import {
  listarInscripcionesCursadaPorAlumno,
  type InscripcionCursadaResponse,
} from '../../api/inscripcionCursadaApi';
import { listarInscripcionesFinalPorAlumno, type InscripcionFinalResponse } from '../../api/inscripcionFinalApi';

export function FichaAlumnoPage() {
  const { id } = useParams<{ id: string }>();
  const alumnoId = Number(id);
  const [tab, setTab] = useState(0);

  const alumnoQuery = useQuery({ queryKey: ['alumno', alumnoId], queryFn: () => obtenerAlumnoPorId(alumnoId) });
  const historialQuery = useQuery({
    queryKey: ['historial', alumnoId],
    queryFn: () => obtenerHistorialPorId(alumnoId),
  });
  const cursadaQuery = useQuery({
    queryKey: ['inscripciones-cursada', 'alumno', alumnoId],
    queryFn: () => listarInscripcionesCursadaPorAlumno(alumnoId),
    enabled: tab === 1,
  });
  const finalQuery = useQuery({
    queryKey: ['inscripciones-final', 'alumno', alumnoId],
    queryFn: () => listarInscripcionesFinalPorAlumno(alumnoId),
    enabled: tab === 2,
  });

  const columnasHistorial: DataTableColumn<HistorialAcademicoResponse>[] = [
    { key: 'materiaCodigo', header: 'Código', render: (h) => h.materiaCodigo },
    { key: 'materiaNombre', header: 'Materia', render: (h) => h.materiaNombre },
    { key: 'estado', header: 'Estado', render: (h) => <Chip label={h.estado} size="small" /> },
    {
      key: 'fecha',
      header: 'Última actualización',
      render: (h) => new Date(h.fechaActualizacion).toLocaleString('es-AR'),
    },
  ];

  const columnasCursada: DataTableColumn<InscripcionCursadaResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'comisionNombre', header: 'Comisión', render: (i) => i.comisionNombre },
    { key: 'estado', header: 'Estado', render: (i) => <Chip label={i.estado} size="small" /> },
    {
      key: 'fecha',
      header: 'Fecha de inscripción',
      render: (i) => new Date(i.fechaInscripcion).toLocaleString('es-AR'),
    },
  ];

  const columnasFinal: DataTableColumn<InscripcionFinalResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'fechaMesa', header: 'Fecha de mesa', render: (i) => new Date(i.fechaMesa).toLocaleDateString('es-AR') },
    { key: 'turno', header: 'Turno', render: (i) => i.turno },
    { key: 'estado', header: 'Estado', render: (i) => <Chip label={i.estado} size="small" /> },
  ];

  if (alumnoQuery.isLoading) return <CircularProgress />;
  if (alumnoQuery.isError || !alumnoQuery.data) {
    return <Alert severity="error">No se pudo cargar la ficha del alumno.</Alert>;
  }

  const alumno = alumnoQuery.data;

  return (
    <Stack spacing={3}>
      <Typography variant="h4">
        {alumno.nombre} {alumno.apellido}
      </Typography>
      <Card>
        <CardContent>
          <Typography>
            <strong>DNI:</strong> {alumno.dni}
          </Typography>
          <Typography>
            <strong>Legajo:</strong> {alumno.legajo}
          </Typography>
          <Typography>
            <strong>Fecha de ingreso:</strong> {new Date(alumno.fechaIngreso).toLocaleDateString('es-AR')}
          </Typography>
        </CardContent>
      </Card>

      <FileDownloadButton
        url={`/certificados/alumno/${alumnoId}/alumno-regular`}
        nombreArchivo={`alumno-regular-${alumno.legajo}.pdf`}
        label="Descargar certificado de alumno regular"
        variant="outlined"
        sx={{ alignSelf: 'flex-start' }}
      />

      <Tabs value={tab} onChange={(_, value) => setTab(value)}>
        <Tab label="Estado académico" />
        <Tab label="Inscripciones a cursadas" />
        <Tab label="Inscripciones a finales" />
      </Tabs>

      {tab === 0 && (
        <>
          {historialQuery.isLoading && <CircularProgress />}
          {historialQuery.isError && <Alert severity="error">No se pudo cargar el estado académico.</Alert>}
          {historialQuery.data && (
            <DataTable columns={columnasHistorial} rows={historialQuery.data} getRowKey={(h) => h.materiaCodigo} />
          )}
        </>
      )}
      {tab === 1 && (
        <>
          {cursadaQuery.isLoading && <CircularProgress />}
          {cursadaQuery.isError && <Alert severity="error">No se pudieron cargar las inscripciones a cursada.</Alert>}
          {cursadaQuery.data && cursadaQuery.data.length === 0 && (
            <Alert severity="info">Sin inscripciones a cursada.</Alert>
          )}
          {cursadaQuery.data && cursadaQuery.data.length > 0 && (
            <DataTable columns={columnasCursada} rows={cursadaQuery.data} getRowKey={(i) => i.id} />
          )}
        </>
      )}
      {tab === 2 && (
        <>
          {finalQuery.isLoading && <CircularProgress />}
          {finalQuery.isError && <Alert severity="error">No se pudieron cargar las inscripciones a final.</Alert>}
          {finalQuery.data && finalQuery.data.length === 0 && (
            <Alert severity="info">Sin inscripciones a final.</Alert>
          )}
          {finalQuery.data && finalQuery.data.length > 0 && (
            <DataTable columns={columnasFinal} rows={finalQuery.data} getRowKey={(i) => i.id} />
          )}
        </>
      )}
    </Stack>
  );
}
