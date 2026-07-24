import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Card, CardContent, CircularProgress, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { listarComisiones } from '../../api/comisionesApi';
import { listarMesasExamen } from '../../api/mesasExamenApi';

export function ExportacionesPage() {
  const [comisionId, setComisionId] = useState<number | ''>('');
  const [mesaId, setMesaId] = useState<number | ''>('');
  const [legajo, setLegajo] = useState('');
  const [dni, setDni] = useState('');
  const [apellido, setApellido] = useState('');

  const comisionesQuery = useQuery({ queryKey: ['comisiones'], queryFn: listarComisiones });
  const mesasQuery = useQuery({ queryKey: ['mesas-examen'], queryFn: listarMesasExamen });

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Exportaciones</Typography>

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Inscriptos por comisión</Typography>
            {comisionesQuery.isLoading && <CircularProgress />}
            {comisionesQuery.isError && <Alert severity="error">No se pudieron cargar las comisiones.</Alert>}
            {comisionesQuery.data && (
              <TextField
                select
                label="Comisión"
                value={comisionId}
                onChange={(e) => setComisionId(e.target.value === '' ? '' : Number(e.target.value))}
                sx={{ maxWidth: 400 }}
              >
                {comisionesQuery.data.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.materiaNombre} — {c.nombre} ({c.periodoLectivo})
                  </MenuItem>
                ))}
              </TextField>
            )}
            <FileDownloadButton
              url="/exportaciones/inscripciones-cursada"
              params={{ comisionId: comisionId === '' ? undefined : comisionId }}
              nombreArchivo="inscripciones-cursada.xlsx"
              label="Descargar Excel"
              variant="contained"
              disabled={comisionId === ''}
              sx={{ alignSelf: 'flex-start' }}
            />
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Inscriptos por mesa de examen</Typography>
            {mesasQuery.isLoading && <CircularProgress />}
            {mesasQuery.isError && <Alert severity="error">No se pudieron cargar las mesas de examen.</Alert>}
            {mesasQuery.data && (
              <TextField
                select
                label="Mesa de examen"
                value={mesaId}
                onChange={(e) => setMesaId(e.target.value === '' ? '' : Number(e.target.value))}
                sx={{ maxWidth: 400 }}
              >
                {mesasQuery.data.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.materiaNombre} — {new Date(m.fecha).toLocaleDateString('es-AR')} ({m.turno})
                  </MenuItem>
                ))}
              </TextField>
            )}
            <FileDownloadButton
              url="/exportaciones/inscripciones-final"
              params={{ mesaId: mesaId === '' ? undefined : mesaId }}
              nombreArchivo="inscripciones-final.xlsx"
              label="Descargar Excel"
              variant="contained"
              disabled={mesaId === ''}
              sx={{ alignSelf: 'flex-start' }}
            />
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Listado general de alumnos</Typography>
            <Stack direction="row" spacing={2}>
              <TextField label="Legajo" value={legajo} onChange={(e) => setLegajo(e.target.value)} size="small" />
              <TextField label="DNI" value={dni} onChange={(e) => setDni(e.target.value)} size="small" />
              <TextField
                label="Apellido"
                value={apellido}
                onChange={(e) => setApellido(e.target.value)}
                size="small"
              />
            </Stack>
            <FileDownloadButton
              url="/exportaciones/alumnos"
              params={{ legajo: legajo || undefined, dni: dni || undefined, apellido: apellido || undefined }}
              nombreArchivo="alumnos.xlsx"
              label="Descargar Excel"
              variant="contained"
              sx={{ alignSelf: 'flex-start' }}
            />
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
