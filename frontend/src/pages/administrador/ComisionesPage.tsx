import { useState } from 'react';
import { useForm, useFieldArray, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ScheduleIcon from '@mui/icons-material/Schedule';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { listarMaterias } from '../../api/materiasApi';
import {
  actualizarComision,
  agregarHorario,
  crearComision,
  eliminarComision,
  listarComisiones,
  quitarHorario,
  type ComisionResponse,
  type HorarioResponse,
} from '../../api/comisionesApi';

const DIAS_SEMANA = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

const horarioSchema = z.object({
  diaSemana: z.coerce.number().int().min(1).max(7),
  horaInicio: z.string().min(1, 'Obligatorio'),
  horaFin: z.string().min(1, 'Obligatorio'),
  aula: z.string().min(1, 'Obligatorio'),
});

const comisionSchema = z.object({
  materiaId: z.coerce.number().int().positive('Elegí una materia'),
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  periodoLectivo: z.string().min(1, 'El período es obligatorio'),
  cupoMaximo: z.coerce.number().int().min(1, 'Debe ser mayor a 0'),
  profesor: z.string().optional(),
  horarios: z.array(horarioSchema),
});

type ComisionFormInput = z.input<typeof comisionSchema>;
type ComisionFormValues = z.output<typeof comisionSchema>;

const horarioNuevoSchema = z.object({
  diaSemana: z.coerce.number().int().min(1).max(7),
  horaInicio: z.string().min(1, 'Obligatorio'),
  horaFin: z.string().min(1, 'Obligatorio'),
  aula: z.string().min(1, 'Obligatorio'),
});
type HorarioNuevoInput = z.input<typeof horarioNuevoSchema>;
type HorarioNuevoValues = z.output<typeof horarioNuevoSchema>;

export function ComisionesPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });
  const { data, isLoading, isError } = useQuery({ queryKey: ['comisiones'], queryFn: listarComisiones });

  const [modalComision, setModalComision] = useState(false);
  const [comisionEnEdicion, setComisionEnEdicion] = useState<ComisionResponse | null>(null);
  const [comisionAEliminar, setComisionAEliminar] = useState<ComisionResponse | null>(null);
  const [comisionHorarios, setComisionHorarios] = useState<ComisionResponse | null>(null);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ComisionFormInput, unknown, ComisionFormValues>({
    resolver: zodResolver(comisionSchema),
    defaultValues: { horarios: [] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'horarios' });

  const horarioForm = useForm<HorarioNuevoInput, unknown, HorarioNuevoValues>({
    resolver: zodResolver(horarioNuevoSchema),
  });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['comisiones'] });

  const crearMutation = useMutation({
    mutationFn: crearComision,
    onSuccess: () => {
      notify('Comisión creada correctamente', 'success');
      invalidar();
      setModalComision(false);
    },
    onError: () => notify('No se pudo crear la comisión (¿duplicada u horarios superpuestos?)', 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: ComisionFormValues }) => actualizarComision(id, request),
    onSuccess: () => {
      notify('Comisión actualizada correctamente', 'success');
      invalidar();
      setModalComision(false);
    },
    onError: () => notify('No se pudo actualizar la comisión', 'error'),
  });

  const eliminarMutation = useMutation({
    mutationFn: eliminarComision,
    onSuccess: () => {
      notify('Comisión eliminada', 'success');
      invalidar();
      setComisionAEliminar(null);
    },
    onError: () => notify('No se pudo eliminar la comisión', 'error'),
  });

  const agregarHorarioMutation = useMutation({
    mutationFn: ({ comisionId, request }: { comisionId: number; request: HorarioNuevoValues }) =>
      agregarHorario(comisionId, request),
    onSuccess: (actualizada) => {
      notify('Horario agregado correctamente', 'success');
      invalidar();
      setComisionHorarios(actualizada);
      horarioForm.reset({ diaSemana: 1, horaInicio: '', horaFin: '', aula: '' });
    },
    onError: () => notify('No se pudo agregar el horario (¿se superpone con otro?)', 'error'),
  });

  const quitarHorarioMutation = useMutation({
    mutationFn: ({ comisionId, horarioId }: { comisionId: number; horarioId: number }) =>
      quitarHorario(comisionId, horarioId),
    onSuccess: (actualizada) => {
      notify('Horario eliminado', 'success');
      invalidar();
      setComisionHorarios(actualizada);
    },
    onError: () => notify('No se pudo eliminar el horario', 'error'),
  });

  const abrirAlta = () => {
    setComisionEnEdicion(null);
    reset({ materiaId: undefined, nombre: '', periodoLectivo: '', cupoMaximo: 0, profesor: '', horarios: [] });
    setModalComision(true);
  };

  const abrirEdicion = (comision: ComisionResponse) => {
    setComisionEnEdicion(comision);
    reset({
      materiaId: comision.materiaId,
      nombre: comision.nombre,
      periodoLectivo: comision.periodoLectivo,
      cupoMaximo: comision.cupoMaximo,
      profesor: comision.profesor ?? '',
      horarios: [],
    });
    setModalComision(true);
  };

  const onSubmitComision = (values: ComisionFormValues) => {
    if (comisionEnEdicion) {
      actualizarMutation.mutate({ id: comisionEnEdicion.id, request: values });
    } else {
      crearMutation.mutate(values);
    }
  };

  const columnas: DataTableColumn<ComisionResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (c) => c.materiaNombre },
    { key: 'nombre', header: 'Comisión', render: (c) => c.nombre },
    { key: 'periodoLectivo', header: 'Período', render: (c) => c.periodoLectivo },
    { key: 'cupoMaximo', header: 'Cupo', render: (c) => c.cupoMaximo },
    { key: 'profesor', header: 'Profesor', render: (c) => c.profesor ?? '—' },
    { key: 'horarios', header: 'Horarios', render: (c) => c.horarios.length },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => (
        <Stack direction="row">
          <IconButton onClick={() => setComisionHorarios(c)} size="small" title="Gestionar horarios">
            <ScheduleIcon fontSize="small" />
          </IconButton>
          <IconButton onClick={() => abrirEdicion(c)} size="small">
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton onClick={() => setComisionAEliminar(c)} size="small">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Comisiones</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nueva comisión
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudieron cargar las comisiones.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(c) => c.id} />}

      <FormModal
        open={modalComision}
        titulo={comisionEnEdicion ? 'Editar comisión' : 'Nueva comisión'}
        onClose={() => setModalComision(false)}
        onSubmit={handleSubmit(onSubmitComision)}
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
              >
                {(materiasQuery.data ?? []).map((materia) => (
                  <MenuItem key={materia.id} value={materia.id}>
                    {materia.codigo} — {materia.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="Nombre de la comisión"
            fullWidth
            error={!!errors.nombre}
            helperText={errors.nombre?.message}
            {...register('nombre')}
          />
          <TextField
            label="Período lectivo"
            fullWidth
            placeholder="2026-1C"
            error={!!errors.periodoLectivo}
            helperText={errors.periodoLectivo?.message}
            {...register('periodoLectivo')}
          />
          <TextField
            label="Cupo máximo"
            type="number"
            fullWidth
            error={!!errors.cupoMaximo}
            helperText={errors.cupoMaximo?.message}
            {...register('cupoMaximo')}
          />
          <TextField label="Profesor" fullWidth {...register('profesor')} />

          {!comisionEnEdicion && (
            <>
              <Divider />
              <Typography variant="subtitle2">Horarios</Typography>
              {fields.map((field, index) => (
                <Stack key={field.id} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Controller
                    name={`horarios.${index}.diaSemana`}
                    control={control}
                    render={({ field: dia }) => (
                      <TextField select label="Día" size="small" sx={{ minWidth: 120 }} {...dia}>
                        {DIAS_SEMANA.map((nombre, i) => (
                          <MenuItem key={nombre} value={i + 1}>
                            {nombre}
                          </MenuItem>
                        ))}
                      </TextField>
                    )}
                  />
                  <TextField
                    label="Inicio"
                    type="time"
                    size="small"
                    slotProps={{ inputLabel: { shrink: true } }}
                    {...register(`horarios.${index}.horaInicio`)}
                  />
                  <TextField
                    label="Fin"
                    type="time"
                    size="small"
                    slotProps={{ inputLabel: { shrink: true } }}
                    {...register(`horarios.${index}.horaFin`)}
                  />
                  <TextField label="Aula" size="small" {...register(`horarios.${index}.aula`)} />
                  <IconButton onClick={() => remove(index)} size="small">
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
              <Button
                startIcon={<AddIcon />}
                onClick={() => append({ diaSemana: 1, horaInicio: '', horaFin: '', aula: '' })}
                sx={{ alignSelf: 'flex-start' }}
              >
                Agregar horario
              </Button>
            </>
          )}
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!comisionAEliminar}
        titulo="Eliminar comisión"
        mensaje={`¿Seguro que querés eliminar la comisión "${comisionAEliminar?.nombre}"?`}
        onConfirm={() => comisionAEliminar && eliminarMutation.mutate(comisionAEliminar.id)}
        onCancel={() => setComisionAEliminar(null)}
      />

      <FormModal
        open={!!comisionHorarios}
        titulo={`Horarios — ${comisionHorarios?.nombre ?? ''}`}
        onClose={() => setComisionHorarios(null)}
        onSubmit={horarioForm.handleSubmit((values) => {
          if (comisionHorarios) agregarHorarioMutation.mutate({ comisionId: comisionHorarios.id, request: values });
        })}
        submitLabel="Agregar horario"
        submitting={agregarHorarioMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          {(comisionHorarios?.horarios ?? []).map((horario: HorarioResponse) => (
            <Stack key={horario.id} direction="row" spacing={2} sx={{ alignItems: 'center' }}>
              <Typography sx={{ flexGrow: 1 }}>
                {DIAS_SEMANA[horario.diaSemana - 1]} {horario.horaInicio}–{horario.horaFin} ({horario.aula})
              </Typography>
              <IconButton
                size="small"
                onClick={() =>
                  comisionHorarios &&
                  quitarHorarioMutation.mutate({ comisionId: comisionHorarios.id, horarioId: horario.id })
                }
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Stack>
          ))}
          <Divider />
          <Typography variant="subtitle2">Agregar horario</Typography>
          <Stack direction="row" spacing={1}>
            <Controller
              name="diaSemana"
              control={horarioForm.control}
              render={({ field }) => (
                <TextField select label="Día" size="small" sx={{ minWidth: 120 }} {...field}>
                  {DIAS_SEMANA.map((nombre, i) => (
                    <MenuItem key={nombre} value={i + 1}>
                      {nombre}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <TextField
              label="Inicio"
              type="time"
              size="small"
              slotProps={{ inputLabel: { shrink: true } }}
              {...horarioForm.register('horaInicio')}
            />
            <TextField
              label="Fin"
              type="time"
              size="small"
              slotProps={{ inputLabel: { shrink: true } }}
              {...horarioForm.register('horaFin')}
            />
            <TextField label="Aula" size="small" {...horarioForm.register('aula')} />
          </Stack>
        </Stack>
      </FormModal>
    </Stack>
  );
}
