import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Alert, Button, CircularProgress, MenuItem, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EstadoChip } from '../../components/EstadoChip';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { aInputDatetime, ETIQUETA_TURNO, formatearFecha, formatearFechaHora, formatearHora } from '../../utils/fechas';
import { listarMaterias } from '../../api/materiasApi';
import {
  actualizarMesaExamen,
  cancelarMesaExamen,
  cerrarMesaExamen,
  crearMesaExamen,
  eliminarMesaExamen,
  listarMesasExamen,
  publicarMesaExamen,
  type EstadoMesa,
  type MesaExamenRequest,
  type MesaExamenResponse,
} from '../../api/mesasExamenApi';

const schema = z.object({
  materiaId: z.coerce.number().int().positive('Elegí una materia'),
  fecha: z.string().min(1, 'La fecha es obligatoria'),
  hora: z.string().optional(),
  turno: z.enum(['MANANA', 'TARDE', 'NOCHE']),
  periodo: z.string().max(20, 'Máximo 20 caracteres').optional(),
  aula: z.string().max(100, 'Máximo 100 caracteres').optional(),
  modalidad: z.enum(['', 'PRESENCIAL', 'VIRTUAL']).optional(),
  cupoMaximo: z.coerce.number().int().min(1, 'Debe ser mayor a 0'),
  profesor: z.string().optional(),
  aperturaInscripcion: z.string().optional(),
  cierreInscripcion: z.string().optional(),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

type Accion = 'publicar' | 'cerrar' | 'cancelar' | 'eliminar';

const TEXTO_ACCION: Record<Accion, { titulo: (m: MesaExamenResponse) => string; mensaje: (m: MesaExamenResponse) => string }> = {
  publicar: {
    titulo: () => 'Publicar mesa',
    mensaje: (m) => `Los alumnos podrán inscribirse a "${m.materiaNombre}" del ${formatearFecha(m.fecha)}. ¿Publicar?`,
  },
  cerrar: {
    titulo: () => 'Cerrar inscripción',
    mensaje: (m) => `Se cerrará la inscripción a "${m.materiaNombre}" del ${formatearFecha(m.fecha)}. Los inscriptos se conservan. ¿Cerrar?`,
  },
  cancelar: {
    titulo: () => 'Cancelar mesa',
    mensaje: (m) =>
      `Se cancelará la mesa de "${m.materiaNombre}" del ${formatearFecha(m.fecha)}${m.inscriptos > 0 ? ` y se darán de baja sus ${m.inscriptos} inscripciones (se conserva el historial)` : ''}. Esta acción no se puede deshacer. ¿Cancelar?`,
  },
  eliminar: {
    titulo: () => 'Eliminar borrador',
    mensaje: (m) => `Se eliminará el borrador de "${m.materiaNombre}" del ${formatearFecha(m.fecha)}. ¿Eliminar?`,
  },
};

function aRequest(v: FormValues): MesaExamenRequest {
  return {
    materiaId: v.materiaId,
    fecha: v.fecha,
    hora: v.hora ? v.hora : null,
    turno: v.turno,
    periodo: v.periodo?.trim() || undefined,
    aula: v.aula?.trim() || undefined,
    modalidad: v.modalidad ? v.modalidad : null,
    cupoMaximo: v.cupoMaximo,
    profesor: v.profesor?.trim() || undefined,
    aperturaInscripcion: v.aperturaInscripcion ? v.aperturaInscripcion : null,
    cierreInscripcion: v.cierreInscripcion ? v.cierreInscripcion : null,
  };
}

export function MesasExamenPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [filtroEstado, setFiltroEstado] = useState<'' | EstadoMesa>('');
  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['mesas-examen', 'admin', filtroEstado],
    queryFn: () => listarMesasExamen(filtroEstado ? { estado: filtroEstado } : {}),
  });

  const [modalAbierto, setModalAbierto] = useState(false);
  const [mesaEnEdicion, setMesaEnEdicion] = useState<MesaExamenResponse | null>(null);
  const [pendiente, setPendiente] = useState<{ mesa: MesaExamenResponse; accion: Accion } | null>(null);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['mesas-examen'] });

  const crearMutation = useMutation({
    mutationFn: crearMesaExamen,
    onSuccess: () => {
      notify('Mesa creada en borrador. Publicala para que los alumnos puedan inscribirse.', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: (err) => notify(extraerMensajeError(err, 'No se pudo crear la mesa'), 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: MesaExamenRequest }) => actualizarMesaExamen(id, request),
    onSuccess: () => {
      notify('Mesa actualizada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: (err) => notify(extraerMensajeError(err, 'No se pudo actualizar la mesa'), 'error'),
  });

  const accionMutation = useMutation({
    mutationFn: async ({ mesa, accion }: { mesa: MesaExamenResponse; accion: Accion }) => {
      if (accion === 'publicar') await publicarMesaExamen(mesa.id);
      else if (accion === 'cerrar') await cerrarMesaExamen(mesa.id);
      else if (accion === 'cancelar') await cancelarMesaExamen(mesa.id);
      else await eliminarMesaExamen(mesa.id);
    },
    onSuccess: (_resultado, { accion }) => {
      const hecho = { publicar: 'publicada', cerrar: 'cerrada', cancelar: 'cancelada', eliminar: 'eliminada' }[accion];
      notify(`Mesa ${hecho} correctamente`, 'success');
      invalidar();
      setPendiente(null);
    },
    onError: (err) => {
      notify(extraerMensajeError(err, 'No se pudo completar la acción'), 'error');
      setPendiente(null);
    },
  });

  const abrirAlta = () => {
    setMesaEnEdicion(null);
    reset({
      materiaId: undefined,
      fecha: '',
      hora: '',
      turno: 'MANANA',
      periodo: '',
      aula: '',
      modalidad: '',
      cupoMaximo: 30,
      profesor: '',
      aperturaInscripcion: '',
      cierreInscripcion: '',
    });
    setModalAbierto(true);
  };

  const abrirEdicion = (mesa: MesaExamenResponse) => {
    setMesaEnEdicion(mesa);
    reset({
      materiaId: mesa.materiaId,
      fecha: mesa.fecha,
      hora: mesa.hora ? mesa.hora.slice(0, 5) : '',
      turno: mesa.turno as FormValues['turno'],
      periodo: mesa.periodo ?? '',
      aula: mesa.aula ?? '',
      modalidad: mesa.modalidad ?? '',
      cupoMaximo: mesa.cupoMaximo,
      profesor: mesa.profesor ?? '',
      aperturaInscripcion: aInputDatetime(mesa.aperturaInscripcion),
      cierreInscripcion: aInputDatetime(mesa.cierreInscripcion),
    });
    setModalAbierto(true);
  };

  const onSubmit = (values: FormValues) => {
    const request = aRequest(values);
    if (mesaEnEdicion) {
      actualizarMutation.mutate({ id: mesaEnEdicion.id, request });
    } else {
      crearMutation.mutate(request);
    }
  };

  const columnas: DataTableColumn<MesaExamenResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (m) => m.materiaNombre },
    {
      key: 'fecha',
      header: 'Fecha y hora',
      render: (m) => `${formatearFecha(m.fecha)} ${m.hora ? formatearHora(m.hora) : ''} · ${ETIQUETA_TURNO[m.turno] ?? m.turno}`,
    },
    { key: 'periodo', header: 'Período', render: (m) => m.periodo ?? '—' },
    { key: 'aula', header: 'Aula / modalidad', render: (m) => [m.aula, m.modalidad].filter(Boolean).join(' · ') || '—' },
    {
      key: 'ventana',
      header: 'Inscripción',
      render: (m) =>
        m.aperturaInscripcion && m.cierreInscripcion
          ? `${formatearFechaHora(m.aperturaInscripcion)} → ${formatearFechaHora(m.cierreInscripcion)}`
          : 'Período general',
    },
    { key: 'cupo', header: 'Inscriptos / cupo', render: (m) => `${m.inscriptos} / ${m.cupoMaximo} (${m.cupoDisponible} libres)` },
    { key: 'estado', header: 'Estado', render: (m) => <EstadoChip estado={m.estado} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => (
        <Stack direction="row" spacing={0.5} sx={{ flexWrap: 'wrap', rowGap: 0.5 }}>
          {(m.estado === 'BORRADOR' || m.estado === 'PUBLICADA') && (
            <Button size="small" onClick={() => abrirEdicion(m)}>
              Editar
            </Button>
          )}
          {(m.estado === 'BORRADOR' || m.estado === 'CERRADA') && (
            <Button size="small" color="success" onClick={() => setPendiente({ mesa: m, accion: 'publicar' })}>
              {m.estado === 'CERRADA' ? 'Reabrir' : 'Publicar'}
            </Button>
          )}
          {m.estado === 'PUBLICADA' && (
            <Button size="small" color="warning" onClick={() => setPendiente({ mesa: m, accion: 'cerrar' })}>
              Cerrar inscripción
            </Button>
          )}
          {m.estado !== 'CANCELADA' && (
            <Button size="small" color="error" onClick={() => setPendiente({ mesa: m, accion: 'cancelar' })}>
              Cancelar mesa
            </Button>
          )}
          {m.estado === 'BORRADOR' && (
            <Button size="small" color="error" onClick={() => setPendiente({ mesa: m, accion: 'eliminar' })}>
              Eliminar
            </Button>
          )}
          <Button size="small" component={Link} to={`/administrador/mesas-examen/${m.id}/inscriptos`}>
            Inscriptos
          </Button>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Mesas de examen final</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nueva mesa
        </Button>
      </Stack>

      <TextField
        select
        size="small"
        label="Filtrar por estado"
        value={filtroEstado}
        onChange={(e) => setFiltroEstado(e.target.value as '' | EstadoMesa)}
        sx={{ maxWidth: 240 }}
      >
        <MenuItem value="">Todos</MenuItem>
        <MenuItem value="BORRADOR">Borrador</MenuItem>
        <MenuItem value="PUBLICADA">Publicada</MenuItem>
        <MenuItem value="CERRADA">Cerrada</MenuItem>
        <MenuItem value="CANCELADA">Cancelada</MenuItem>
      </TextField>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">{extraerMensajeError(error, 'No se pudieron cargar las mesas de examen.')}</Alert>}
      {data && data.length === 0 && <Alert severity="info">No hay mesas de examen con ese criterio.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(m) => m.id} />}

      <FormModal
        open={modalAbierto}
        titulo={mesaEnEdicion ? 'Editar mesa de examen' : 'Nueva mesa de examen'}
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit(onSubmit)}
        submitting={crearMutation.isPending || actualizarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Controller
            name="materiaId"
            control={control}
            render={({ field }) => (
              <TextField
                select
                label="Materia"
                fullWidth
                error={!!errors.materiaId}
                helperText={errors.materiaId?.message}
                {...field}
                value={field.value ?? ''}
              >
                {(materiasQuery.data ?? []).map((materia) => (
                  <MenuItem key={materia.id} value={materia.id}>
                    {materia.codigo} — {materia.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              label="Fecha"
              type="date"
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              error={!!errors.fecha}
              helperText={errors.fecha?.message}
              {...register('fecha')}
            />
            <TextField label="Hora" type="time" fullWidth slotProps={{ inputLabel: { shrink: true } }} {...register('hora')} />
          </Stack>
          <Controller
            name="turno"
            control={control}
            render={({ field }) => (
              <TextField select label="Turno" fullWidth {...field}>
                <MenuItem value="MANANA">Mañana</MenuItem>
                <MenuItem value="TARDE">Tarde</MenuItem>
                <MenuItem value="NOCHE">Noche</MenuItem>
              </TextField>
            )}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              label="Período académico (ej. 2026-2)"
              fullWidth
              error={!!errors.periodo}
              helperText={errors.periodo?.message}
              {...register('periodo')}
            />
            <TextField label="Aula / sede" fullWidth error={!!errors.aula} helperText={errors.aula?.message} {...register('aula')} />
          </Stack>
          <Controller
            name="modalidad"
            control={control}
            render={({ field }) => (
              <TextField select label="Modalidad" fullWidth {...field} value={field.value ?? ''}>
                <MenuItem value="">Sin especificar</MenuItem>
                <MenuItem value="PRESENCIAL">Presencial</MenuItem>
                <MenuItem value="VIRTUAL">Virtual</MenuItem>
              </TextField>
            )}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              label="Cupo máximo"
              type="number"
              fullWidth
              error={!!errors.cupoMaximo}
              helperText={errors.cupoMaximo?.message}
              {...register('cupoMaximo')}
            />
            <TextField label="Profesor" fullWidth {...register('profesor')} />
          </Stack>
          <Typography variant="caption" color="text.secondary">
            Ventana de inscripción propia de la mesa (opcional). Si la dejás vacía rige el período general de
            finales de Configuración. Apertura y cierre se indican juntos.
          </Typography>
          <Stack direction="row" spacing={2}>
            <TextField
              label="Apertura de inscripción"
              type="datetime-local"
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              {...register('aperturaInscripcion')}
            />
            <TextField
              label="Cierre de inscripción"
              type="datetime-local"
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              {...register('cierreInscripcion')}
            />
          </Stack>
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!pendiente}
        titulo={pendiente ? TEXTO_ACCION[pendiente.accion].titulo(pendiente.mesa) : ''}
        mensaje={pendiente ? TEXTO_ACCION[pendiente.accion].mensaje(pendiente.mesa) : ''}
        onConfirm={() => pendiente && accionMutation.mutate(pendiente)}
        onCancel={() => setPendiente(null)}
      />
    </Stack>
  );
}
