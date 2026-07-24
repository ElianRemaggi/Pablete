import { useMemo, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  FormControlLabel,
  IconButton,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { useSnackbar } from '../../components/SnackbarProvider';
import { listarCarreras } from '../../api/carrerasApi';
import { listarMaterias, listarMateriasPorPlan, type MateriaResponse } from '../../api/materiasApi';
import {
  actualizarPlanEstudio,
  asociarMateriaAPlan,
  crearPlanEstudio,
  listarPlanesDeCarrera,
  type PlanEstudioResponse,
} from '../../api/planesEstudioApi';

const planSchema = z.object({
  version: z.string().min(1, 'La versión es obligatoria'),
  vigente: z.boolean(),
});
type PlanFormValues = z.infer<typeof planSchema>;

const asociarSchema = z.object({
  materiaId: z.coerce.number().int().positive('Elegí una materia'),
  anio: z.coerce.number().int().min(1, 'Debe ser mayor a 0'),
  cuatrimestre: z.coerce.number().int().min(1).max(2),
});
type AsociarFormInput = z.input<typeof asociarSchema>;
type AsociarFormValues = z.output<typeof asociarSchema>;

export function PlanesEstudioPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();

  const carrerasQuery = useQuery({ queryKey: ['carreras'], queryFn: listarCarreras });
  const [carreraId, setCarreraId] = useState<number | ''>('');
  const [planSeleccionado, setPlanSeleccionado] = useState<PlanEstudioResponse | null>(null);
  const [modalPlan, setModalPlan] = useState(false);
  const [planEnEdicion, setPlanEnEdicion] = useState<PlanEstudioResponse | null>(null);
  const [modalAsociar, setModalAsociar] = useState(false);

  const planesQuery = useQuery({
    queryKey: ['planes-estudio', carreraId],
    queryFn: () => listarPlanesDeCarrera(carreraId as number),
    enabled: carreraId !== '',
  });

  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });

  const mallaQuery = useQuery({
    queryKey: ['materias-plan', planSeleccionado?.id],
    queryFn: () => listarMateriasPorPlan(planSeleccionado!.id),
    enabled: !!planSeleccionado,
  });

  const planForm = useForm<PlanFormValues>({ resolver: zodResolver(planSchema), defaultValues: { vigente: true } });
  const asociarForm = useForm<AsociarFormInput, unknown, AsociarFormValues>({
    resolver: zodResolver(asociarSchema),
  });

  const invalidarPlanes = () => queryClient.invalidateQueries({ queryKey: ['planes-estudio', carreraId] });

  const crearMutation = useMutation({
    mutationFn: (values: PlanFormValues) => crearPlanEstudio(carreraId as number, values),
    onSuccess: () => {
      notify('Plan de estudio creado correctamente', 'success');
      invalidarPlanes();
      setModalPlan(false);
    },
    onError: () => notify('No se pudo crear el plan (¿versión duplicada?)', 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: PlanFormValues }) => actualizarPlanEstudio(id, values),
    onSuccess: () => {
      notify('Plan de estudio actualizado correctamente', 'success');
      invalidarPlanes();
      setModalPlan(false);
    },
    onError: () => notify('No se pudo actualizar el plan', 'error'),
  });

  const asociarMutation = useMutation({
    mutationFn: (values: AsociarFormValues) =>
      asociarMateriaAPlan(values.materiaId, planSeleccionado!.id, {
        anio: values.anio,
        cuatrimestre: values.cuatrimestre,
      }),
    onSuccess: () => {
      notify('Materia asociada al plan correctamente', 'success');
      queryClient.invalidateQueries({ queryKey: ['materias-plan', planSeleccionado?.id] });
      setModalAsociar(false);
    },
    onError: () => notify('No se pudo asociar la materia (¿ya está asociada?)', 'error'),
  });

  const materiasNoAsociadas = useMemo(() => {
    const idsAsociadas = new Set((mallaQuery.data ?? []).map((m) => m.id));
    return (materiasQuery.data ?? []).filter((m) => !idsAsociadas.has(m.id));
  }, [materiasQuery.data, mallaQuery.data]);

  const abrirAltaPlan = () => {
    setPlanEnEdicion(null);
    planForm.reset({ version: '', vigente: true });
    setModalPlan(true);
  };

  const abrirEdicionPlan = (plan: PlanEstudioResponse) => {
    setPlanEnEdicion(plan);
    planForm.reset({ version: plan.version, vigente: plan.vigente });
    setModalPlan(true);
  };

  const onSubmitPlan = (values: PlanFormValues) => {
    if (planEnEdicion) {
      actualizarMutation.mutate({ id: planEnEdicion.id, values });
    } else {
      crearMutation.mutate(values);
    }
  };

  const columnasPlanes: DataTableColumn<PlanEstudioResponse>[] = [
    { key: 'version', header: 'Versión', render: (p) => p.version },
    {
      key: 'vigente',
      header: 'Estado',
      render: (p) => <Chip label={p.vigente ? 'Vigente' : 'No vigente'} color={p.vigente ? 'success' : 'default'} size="small" />,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (p) => (
        <Stack direction="row">
          <Button size="small" onClick={() => setPlanSeleccionado(p)}>
            Ver malla
          </Button>
          <IconButton onClick={() => abrirEdicionPlan(p)} size="small">
            <EditIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  const columnasMalla: DataTableColumn<MateriaResponse>[] = [
    { key: 'codigo', header: 'Código', render: (m) => m.codigo },
    { key: 'nombre', header: 'Nombre', render: (m) => m.nombre },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Planes de estudio</Typography>

      {carrerasQuery.isLoading && <CircularProgress />}
      {carrerasQuery.isError && <Alert severity="error">No se pudieron cargar las carreras.</Alert>}
      {carrerasQuery.data && (
        <TextField
          select
          label="Carrera"
          value={carreraId}
          onChange={(e) => {
            setCarreraId(e.target.value === '' ? '' : Number(e.target.value));
            setPlanSeleccionado(null);
          }}
          sx={{ maxWidth: 400 }}
        >
          {carrerasQuery.data.map((c) => (
            <MenuItem key={c.id} value={c.id}>
              {c.codigo} — {c.nombre}
            </MenuItem>
          ))}
        </TextField>
      )}

      {carreraId !== '' && (
        <>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Planes</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAltaPlan}>
              Nuevo plan
            </Button>
          </Stack>
          {planesQuery.isLoading && <CircularProgress />}
          {planesQuery.isError && <Alert severity="error">No se pudieron cargar los planes.</Alert>}
          {planesQuery.data && <DataTable columns={columnasPlanes} rows={planesQuery.data} getRowKey={(p) => p.id} />}
        </>
      )}

      {planSeleccionado && (
        <>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Malla — versión {planSeleccionado.version}</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setModalAsociar(true)}>
              Asociar materia
            </Button>
          </Stack>
          {mallaQuery.isLoading && <CircularProgress />}
          {mallaQuery.isError && <Alert severity="error">No se pudo cargar la malla del plan.</Alert>}
          {mallaQuery.data && <DataTable columns={columnasMalla} rows={mallaQuery.data} getRowKey={(m) => m.id} />}
        </>
      )}

      <FormModal
        open={modalPlan}
        titulo={planEnEdicion ? 'Editar plan' : 'Nuevo plan'}
        onClose={() => setModalPlan(false)}
        onSubmit={planForm.handleSubmit(onSubmitPlan)}
        submitting={crearMutation.isPending || actualizarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Versión"
            fullWidth
            error={!!planForm.formState.errors.version}
            helperText={planForm.formState.errors.version?.message}
            {...planForm.register('version')}
          />
          <Controller
            name="vigente"
            control={planForm.control}
            render={({ field }) => (
              <FormControlLabel
                control={<Switch checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                label="Vigente"
              />
            )}
          />
        </Stack>
      </FormModal>

      <FormModal
        open={modalAsociar}
        titulo="Asociar materia al plan"
        onClose={() => setModalAsociar(false)}
        onSubmit={asociarForm.handleSubmit((values) => asociarMutation.mutate(values))}
        submitting={asociarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Controller
            name="materiaId"
            control={asociarForm.control}
            render={({ field }) => (
              <TextField select label="Materia" fullWidth {...field}>
                {materiasNoAsociadas.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.codigo} — {m.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField label="Año" type="number" fullWidth {...asociarForm.register('anio')} />
          <Controller
            name="cuatrimestre"
            control={asociarForm.control}
            render={({ field }) => (
              <TextField select label="Cuatrimestre" fullWidth {...field}>
                <MenuItem value={1}>1°</MenuItem>
                <MenuItem value={2}>2°</MenuItem>
              </TextField>
            )}
          />
        </Stack>
      </FormModal>
    </Stack>
  );
}
