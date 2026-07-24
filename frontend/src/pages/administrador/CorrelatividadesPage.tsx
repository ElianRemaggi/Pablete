import { useMemo, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  CircularProgress,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { listarMaterias } from '../../api/materiasApi';
import {
  crearCorrelatividad,
  eliminarCorrelatividad,
  listarCorrelatividadesPorMateria,
  type CorrelatividadResponse,
} from '../../api/correlatividadesApi';

const schema = z.object({
  materiaRequeridaId: z.coerce.number().int().positive('Elegí una materia requerida'),
  tipoRequisito: z.enum(['CURSAR', 'APROBAR']),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function CorrelatividadesPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });
  const [materiaId, setMateriaId] = useState<number | ''>('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [correlatividadAEliminar, setCorrelatividadAEliminar] = useState<CorrelatividadResponse | null>(null);

  const correlatividadesQuery = useQuery({
    queryKey: ['correlatividades', materiaId],
    queryFn: () => listarCorrelatividadesPorMateria(materiaId as number),
    enabled: materiaId !== '',
  });

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tipoRequisito: 'APROBAR' },
  });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['correlatividades', materiaId] });

  const crearMutation = useMutation({
    mutationFn: crearCorrelatividad,
    onSuccess: () => {
      notify('Correlatividad creada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo crear la correlatividad (¿duplicada o generaría un ciclo?)', 'error'),
  });

  const eliminarMutation = useMutation({
    mutationFn: eliminarCorrelatividad,
    onSuccess: () => {
      notify('Correlatividad eliminada', 'success');
      invalidar();
      setCorrelatividadAEliminar(null);
    },
    onError: () => notify('No se pudo eliminar la correlatividad', 'error'),
  });

  const materiasDisponibles = useMemo(
    () => (materiasQuery.data ?? []).filter((m) => m.id !== materiaId),
    [materiasQuery.data, materiaId],
  );

  const abrirAlta = () => {
    reset({ materiaRequeridaId: undefined, tipoRequisito: 'APROBAR' });
    setModalAbierto(true);
  };

  const onSubmit = (values: FormValues) => {
    if (materiaId === '') return;
    crearMutation.mutate({ materiaId, materiaRequeridaId: values.materiaRequeridaId, tipoRequisito: values.tipoRequisito });
  };

  const columnas: DataTableColumn<CorrelatividadResponse>[] = [
    { key: 'materiaRequeridaNombre', header: 'Materia requerida', render: (c) => c.materiaRequeridaNombre },
    { key: 'tipoRequisito', header: 'Requisito', render: (c) => c.tipoRequisito },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => (
        <IconButton onClick={() => setCorrelatividadAEliminar(c)} size="small">
          <DeleteIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Correlatividades</Typography>

      {materiasQuery.isLoading && <CircularProgress />}
      {materiasQuery.isError && <Alert severity="error">No se pudieron cargar las materias.</Alert>}

      {materiasQuery.data && (
        <TextField
          select
          label="Materia"
          value={materiaId}
          onChange={(e) => setMateriaId(e.target.value === '' ? '' : Number(e.target.value))}
          sx={{ maxWidth: 400 }}
        >
          {materiasQuery.data.map((m) => (
            <MenuItem key={m.id} value={m.id}>
              {m.codigo} — {m.nombre}
            </MenuItem>
          ))}
        </TextField>
      )}

      {materiaId !== '' && (
        <>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Correlativas requeridas</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
              Nueva correlatividad
            </Button>
          </Stack>

          {correlatividadesQuery.isLoading && <CircularProgress />}
          {correlatividadesQuery.isError && (
            <Alert severity="error">No se pudieron cargar las correlatividades.</Alert>
          )}
          {correlatividadesQuery.data && (
            <DataTable columns={columnas} rows={correlatividadesQuery.data} getRowKey={(c) => c.id} />
          )}
        </>
      )}

      <FormModal
        open={modalAbierto}
        titulo="Nueva correlatividad"
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit(onSubmit)}
        submitting={crearMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Controller
            name="materiaRequeridaId"
            control={control}
            render={({ field }) => (
              <TextField
                select
                label="Materia requerida"
                fullWidth
                error={!!errors.materiaRequeridaId}
                helperText={errors.materiaRequeridaId?.message}
                {...field}
              >
                {materiasDisponibles.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.codigo} — {m.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <Controller
            name="tipoRequisito"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo de requisito" fullWidth {...field}>
                <MenuItem value="CURSAR">Cursar</MenuItem>
                <MenuItem value="APROBAR">Aprobar</MenuItem>
              </TextField>
            )}
          />
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!correlatividadAEliminar}
        titulo="Eliminar correlatividad"
        mensaje={`¿Seguro que querés eliminar el requisito sobre "${correlatividadAEliminar?.materiaRequeridaNombre}"?`}
        onConfirm={() => correlatividadAEliminar && eliminarMutation.mutate(correlatividadAEliminar.id)}
        onCancel={() => setCorrelatividadAEliminar(null)}
      />
    </Stack>
  );
}
