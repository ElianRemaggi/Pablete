import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import {
  ETIQUETA_TURNO,
  formatearFecha,
  formatearFechaHora,
  formatearHora,
} from '../../utils/fechas';
import { listarMesasExamen, type MesaExamenResponse } from '../../api/mesasExamenApi';
import { inscribirFinal, listarInscripcionesFinalPropias } from '../../api/inscripcionFinalApi';

/** Por que el boton "Inscribirme" esta deshabilitado (null = se puede intentar). La decision final la toma
 * siempre el backend (correlatividades, regularidad, antelacion, intentos...): esto solo evita clics inutiles. */
function motivoNoDisponible(mesa: MesaExamenResponse, yaInscripto: boolean): string | null {
  if (yaInscripto) return 'Ya estás inscripto/a a esta mesa';
  if (!mesa.inscripcionAbierta) return 'La inscripción a esta mesa no está abierta en este momento';
  if (mesa.cupoDisponible <= 0) return 'No hay cupo disponible';
  return null;
}

function DetalleMesa({ mesa, onClose }: { mesa: MesaExamenResponse | null; onClose: () => void }) {
  const filas: Array<[string, string]> = mesa
    ? [
        ['Materia', mesa.materiaNombre],
        ['Fecha', formatearFecha(mesa.fecha)],
        ['Hora', formatearHora(mesa.hora)],
        ['Turno', ETIQUETA_TURNO[mesa.turno] ?? mesa.turno],
        ['Período académico', mesa.periodo ?? '—'],
        ['Aula / sede', mesa.aula ?? '—'],
        ['Modalidad', mesa.modalidad ?? '—'],
        ['Profesor', mesa.profesor ?? '—'],
        ['Cupo', `${mesa.inscriptos} inscriptos de ${mesa.cupoMaximo} (${mesa.cupoDisponible} lugares libres)`],
        [
          'Inscripción',
          mesa.aperturaInscripcion && mesa.cierreInscripcion
            ? `${formatearFechaHora(mesa.aperturaInscripcion)} a ${formatearFechaHora(mesa.cierreInscripcion)}`
            : 'Según el período general de inscripción a finales',
        ],
      ]
    : [];
  return (
    <Dialog open={!!mesa} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Datos de la mesa</DialogTitle>
      <DialogContent>
        <Stack spacing={1}>
          {filas.map(([etiqueta, valor]) => (
            <Typography key={etiqueta}>
              <strong>{etiqueta}:</strong> {valor}
            </Typography>
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}

export function AlumnoInscripcionFinalPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [filtroMateria, setFiltroMateria] = useState('');
  const [filtroPeriodo, setFiltroPeriodo] = useState('');
  const [filtroFecha, setFiltroFecha] = useState('');
  const [detalle, setDetalle] = useState<MesaExamenResponse | null>(null);

  const mesasQuery = useQuery({ queryKey: ['mesas-examen', 'disponibles'], queryFn: () => listarMesasExamen() });
  const misInscripcionesQuery = useQuery({
    queryKey: ['inscripciones-final', 'me'],
    queryFn: listarInscripcionesFinalPropias,
  });

  const mesasInscriptas = useMemo(
    () =>
      new Set(
        (misInscripcionesQuery.data ?? []).filter((i) => i.estado !== 'BAJA').map((i) => i.mesaId),
      ),
    [misInscripcionesQuery.data],
  );

  const mesasFiltradas = useMemo(
    () =>
      (mesasQuery.data ?? []).filter(
        (m) =>
          (!filtroMateria || m.materiaNombre.toLowerCase().includes(filtroMateria.trim().toLowerCase())) &&
          (!filtroPeriodo || (m.periodo ?? '').toLowerCase().includes(filtroPeriodo.trim().toLowerCase())) &&
          (!filtroFecha || m.fecha === filtroFecha),
      ),
    [mesasQuery.data, filtroMateria, filtroPeriodo, filtroFecha],
  );

  const inscribirMutation = useMutation({
    mutationFn: inscribirFinal,
    onSuccess: (inscripcion) => {
      notify(
        `Inscripción confirmada: ${inscripcion.materiaNombre} (${formatearFecha(inscripcion.fechaMesa)}). Podés descargar el comprobante en "Mis inscripciones".`,
        'success',
      );
      queryClient.invalidateQueries({ queryKey: ['inscripciones-final', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['mesas-examen'] });
    },
    onError: (error) => notify(extraerMensajeError(error, 'No se pudo completar la inscripción'), 'error'),
  });

  const columnas: DataTableColumn<MesaExamenResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (m) => m.materiaNombre },
    { key: 'fecha', header: 'Fecha', render: (m) => formatearFecha(m.fecha) },
    { key: 'hora', header: 'Hora', render: (m) => formatearHora(m.hora) },
    { key: 'turno', header: 'Turno', render: (m) => ETIQUETA_TURNO[m.turno] ?? m.turno },
    { key: 'periodo', header: 'Período', render: (m) => m.periodo ?? '—' },
    { key: 'aula', header: 'Aula / modalidad', render: (m) => [m.aula, m.modalidad].filter(Boolean).join(' · ') || '—' },
    { key: 'cupo', header: 'Lugares libres', render: (m) => `${m.cupoDisponible} / ${m.cupoMaximo}` },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => {
        const motivo = motivoNoDisponible(m, mesasInscriptas.has(m.id));
        return (
          <Stack direction="row" spacing={1}>
            <Button size="small" onClick={() => setDetalle(m)}>
              Ver datos
            </Button>
            <Tooltip title={motivo ?? ''} disableHoverListener={!motivo}>
              <span>
                <Button
                  size="small"
                  variant="contained"
                  disabled={!!motivo || inscribirMutation.isPending}
                  onClick={() => inscribirMutation.mutate(m.id)}
                >
                  Inscribirme
                </Button>
              </span>
            </Tooltip>
          </Stack>
        );
      },
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Exámenes finales</Typography>
      <Typography color="text.secondary">
        Mesas publicadas por la institución. Para ver tus inscripciones y descargar los comprobantes en PDF andá a{' '}
        <Link to="/alumno/mis-inscripciones">Mis inscripciones</Link>.
      </Typography>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          label="Materia"
          size="small"
          value={filtroMateria}
          onChange={(e) => setFiltroMateria(e.target.value)}
        />
        <TextField
          label="Período (ej. 2026-2)"
          size="small"
          value={filtroPeriodo}
          onChange={(e) => setFiltroPeriodo(e.target.value)}
        />
        <TextField
          label="Fecha"
          type="date"
          size="small"
          value={filtroFecha}
          onChange={(e) => setFiltroFecha(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <Button
          onClick={() => {
            setFiltroMateria('');
            setFiltroPeriodo('');
            setFiltroFecha('');
          }}
        >
          Limpiar filtros
        </Button>
      </Stack>

      {mesasQuery.isLoading && <CircularProgress />}
      {mesasQuery.isError && (
        <Alert severity="error">
          {extraerMensajeError(mesasQuery.error, 'No se pudieron cargar las mesas de examen.')}
        </Alert>
      )}
      {mesasQuery.data && mesasFiltradas.length === 0 && (
        <Alert severity="info">No hay mesas publicadas que coincidan con los filtros.</Alert>
      )}
      {mesasFiltradas.length > 0 && <DataTable columns={columnas} rows={mesasFiltradas} getRowKey={(m) => m.id} />}

      <DetalleMesa mesa={detalle} onClose={() => setDetalle(null)} />
    </Stack>
  );
}
