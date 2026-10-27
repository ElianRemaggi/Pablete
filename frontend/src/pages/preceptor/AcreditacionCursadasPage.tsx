import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { EstadoChip } from '../../components/EstadoChip';
import { CalificacionDialog, type OpcionResultado } from '../../components/CalificacionDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { formatearFechaHora } from '../../utils/fechas';
import { listarComisiones } from '../../api/comisionesApi';
import {
  calificarCursada,
  listarInscriptosPorComision,
  type InscripcionCursadaResponse,
  type ResultadoCursada,
} from '../../api/inscripcionCursadaApi';

const OPCIONES: OpcionResultado[] = [
  { value: 'APROBADA', label: 'Cursada aprobada', conNota: true },
  { value: 'DESAPROBADA', label: 'Cursada desaprobada', conNota: true },
  { value: 'PENDIENTE', label: 'Pendiente de evaluación', conNota: false },
];

/** Carga de notas de cursada (preceptor y administrador; el backend autoriza ambos roles). Una cursada aprobada
 * habilita al alumno para el final de esa materia, pero NO la marca como final aprobado. */
export function AcreditacionCursadasPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [comisionId, setComisionId] = useState<number | ''>('');
  const [busqueda, setBusqueda] = useState('');
  const [editando, setEditando] = useState<InscripcionCursadaResponse | null>(null);

  const comisionesQuery = useQuery({ queryKey: ['comisiones'], queryFn: listarComisiones });
  const inscriptosQuery = useQuery({
    queryKey: ['inscripciones-cursada', 'comision', comisionId],
    queryFn: () => listarInscriptosPorComision(Number(comisionId)),
    enabled: comisionId !== '',
  });

  const guardar = useMutation({
    mutationFn: (v: { id: number; resultado: ResultadoCursada; nota: number | null }) =>
      calificarCursada(v.id, v.resultado, v.nota),
    onSuccess: (r) => {
      notify(
        `Nota guardada: ${r.alumnoApellido}, ${r.alumnoNombre} — ${r.materiaNombre}: ${r.resultadoCursada}` +
          (r.notaCursada != null ? ` (${r.notaCursada})` : ''),
        'success',
      );
      queryClient.invalidateQueries({ queryKey: ['inscripciones-cursada'] });
      setEditando(null);
    },
    onError: (err) => notify(extraerMensajeError(err, 'No se pudo guardar la nota'), 'error'),
  });

  const filas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return (inscriptosQuery.data ?? []).filter(
      (i) => !q || `${i.alumnoApellido} ${i.alumnoNombre} ${i.alumnoDni} ${i.alumnoLegajo}`.toLowerCase().includes(q),
    );
  }, [inscriptosQuery.data, busqueda]);

  const columnas: DataTableColumn<InscripcionCursadaResponse>[] = [
    { key: 'alumno', header: 'Apellido y nombre', render: (i) => `${i.alumnoApellido}, ${i.alumnoNombre}` },
    { key: 'dni', header: 'DNI', render: (i) => i.alumnoDni },
    { key: 'legajo', header: 'Legajo', render: (i) => i.alumnoLegajo },
    { key: 'inscripcion', header: 'Inscripción', render: (i) => <EstadoChip estado={i.estado} /> },
    { key: 'resultado', header: 'Cursada', render: (i) => <EstadoChip estado={i.resultadoCursada} /> },
    { key: 'nota', header: 'Nota', render: (i) => i.notaCursada ?? '—' },
    {
      key: 'fecha',
      header: 'Calificada el',
      render: (i) => (i.fechaCalificacion ? formatearFechaHora(i.fechaCalificacion) : '—'),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (i) => (
        <Button size="small" disabled={i.estado === 'BAJA'} onClick={() => setEditando(i)}>
          {i.resultadoCursada === 'PENDIENTE' ? 'Cargar nota' : 'Corregir nota'}
        </Button>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Acreditación de cursadas</Typography>
      <Typography color="text.secondary">
        Elegí la comisión, buscá al alumno y cargá la nota de la cursada. Si queda aprobada, el alumno queda habilitado
        para inscribirse al final (si cumple las correlatividades); no se marca el final como aprobado.
      </Typography>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          select
          size="small"
          label="Comisión"
          value={comisionId}
          onChange={(e) => setComisionId(e.target.value === '' ? '' : Number(e.target.value))}
          sx={{ minWidth: 320 }}
        >
          {(comisionesQuery.data ?? []).map((c) => (
            <MenuItem key={c.id} value={c.id}>
              {c.materiaNombre} — {c.nombre} ({c.periodoLectivo})
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label="Buscar por nombre, apellido, DNI o legajo"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          sx={{ minWidth: 320 }}
        />
      </Stack>

      {comisionesQuery.isError && (
        <Alert severity="error">{extraerMensajeError(comisionesQuery.error, 'No se pudieron cargar las comisiones.')}</Alert>
      )}
      {comisionId === '' && <Alert severity="info">Seleccioná una comisión para ver sus inscriptos.</Alert>}
      {inscriptosQuery.isLoading && <CircularProgress />}
      {inscriptosQuery.isError && (
        <Alert severity="error">{extraerMensajeError(inscriptosQuery.error, 'No se pudieron cargar los inscriptos.')}</Alert>
      )}
      {inscriptosQuery.data && filas.length === 0 && <Alert severity="info">No hay inscriptos para mostrar.</Alert>}
      {filas.length > 0 && <DataTable columns={columnas} rows={filas} getRowKey={(i) => i.id} />}

      <CalificacionDialog
        open={!!editando}
        titulo="Nota de cursada"
        descripcion={
          editando
            ? `${editando.alumnoApellido}, ${editando.alumnoNombre} (DNI ${editando.alumnoDni}) — ${editando.materiaNombre}, ` +
              `comisión ${editando.comisionNombre}, período ${editando.periodo}.`
            : ''
        }
        opciones={OPCIONES}
        resultadoInicial={editando?.resultadoCursada ?? 'PENDIENTE'}
        notaInicial={editando?.notaCursada ?? null}
        submitting={guardar.isPending}
        onClose={() => setEditando(null)}
        onSubmit={(resultado, nota) =>
          editando && guardar.mutate({ id: editando.id, resultado: resultado as ResultadoCursada, nota })
        }
      />
    </Stack>
  );
}
