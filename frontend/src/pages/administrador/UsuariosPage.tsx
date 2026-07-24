import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  FormControlLabel,
  FormGroup,
  IconButton,
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
import {
  actualizarRolesUsuario,
  actualizarUsuario,
  activarUsuario,
  crearUsuario,
  desactivarUsuario,
  listarUsuarios,
  type UsuarioResponse,
} from '../../api/usuariosApi';

const ROLES_DISPONIBLES = ['ALUMNO', 'PRECEPTOR', 'ADMINISTRADOR'] as const;

const altaSchema = z.object({
  dni: z.string().min(1, 'El DNI es obligatorio'),
  email: z.string().email('Email inválido'),
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  apellido: z.string().min(1, 'El apellido es obligatorio'),
  password: z
    .string()
    .regex(/^(?=.*[A-Z])(?=.*\d).{8,}$/, 'Al menos 8 caracteres, una mayúscula y un número'),
  roles: z.array(z.enum(ROLES_DISPONIBLES)).min(1, 'Elegí al menos un rol'),
});
type AltaValues = z.infer<typeof altaSchema>;

const edicionSchema = z.object({
  email: z.string().email('Email inválido'),
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  apellido: z.string().min(1, 'El apellido es obligatorio'),
  roles: z.array(z.enum(ROLES_DISPONIBLES)).min(1, 'Elegí al menos un rol'),
});
type EdicionValues = z.infer<typeof edicionSchema>;

function SelectorRoles({
  value,
  onChange,
}: {
  value: string[];
  onChange: (roles: string[]) => void;
}) {
  const toggle = (rol: string) => {
    onChange(value.includes(rol) ? value.filter((r) => r !== rol) : [...value, rol]);
  };

  return (
    <FormGroup row>
      {ROLES_DISPONIBLES.map((rol) => (
        <FormControlLabel
          key={rol}
          control={<Checkbox checked={value.includes(rol)} onChange={() => toggle(rol)} />}
          label={rol}
        />
      ))}
    </FormGroup>
  );
}

export function UsuariosPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const { data, isLoading, isError } = useQuery({ queryKey: ['usuarios'], queryFn: listarUsuarios });

  const [modalAlta, setModalAlta] = useState(false);
  const [usuarioEnEdicion, setUsuarioEnEdicion] = useState<UsuarioResponse | null>(null);

  const altaForm = useForm<AltaValues>({
    resolver: zodResolver(altaSchema),
    defaultValues: { roles: [] },
  });
  const edicionForm = useForm<EdicionValues>({ resolver: zodResolver(edicionSchema) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['usuarios'] });

  const crearMutation = useMutation({
    mutationFn: crearUsuario,
    onSuccess: () => {
      notify('Usuario creado correctamente', 'success');
      invalidar();
      setModalAlta(false);
    },
    onError: () => notify('No se pudo crear el usuario (¿DNI o email duplicado?)', 'error'),
  });

  const editarMutation = useMutation({
    mutationFn: async ({ id, values }: { id: number; values: EdicionValues }) => {
      await actualizarUsuario(id, { email: values.email, nombre: values.nombre, apellido: values.apellido });
      return actualizarRolesUsuario(id, values.roles);
    },
    onSuccess: () => {
      notify('Usuario actualizado correctamente', 'success');
      invalidar();
      setUsuarioEnEdicion(null);
    },
    onError: () => notify('No se pudo actualizar el usuario', 'error'),
  });

  const toggleActivoMutation = useMutation({
    mutationFn: (usuario: UsuarioResponse) => (usuario.activo ? desactivarUsuario(usuario.id) : activarUsuario(usuario.id)),
    onSuccess: () => {
      notify('Estado del usuario actualizado', 'success');
      invalidar();
    },
    onError: () => notify('No se pudo cambiar el estado del usuario', 'error'),
  });

  const abrirAlta = () => {
    altaForm.reset({ dni: '', email: '', nombre: '', apellido: '', password: '', roles: [] });
    setModalAlta(true);
  };

  const abrirEdicion = (usuario: UsuarioResponse) => {
    edicionForm.reset({
      email: usuario.email,
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      roles: usuario.roles as EdicionValues['roles'],
    });
    setUsuarioEnEdicion(usuario);
  };

  const columnas: DataTableColumn<UsuarioResponse>[] = [
    { key: 'dni', header: 'DNI', render: (u) => u.dni },
    { key: 'nombre', header: 'Nombre', render: (u) => `${u.nombre} ${u.apellido}` },
    { key: 'email', header: 'Email', render: (u) => u.email },
    {
      key: 'roles',
      header: 'Roles',
      render: (u) => (
        <Stack direction="row" spacing={0.5}>
          {u.roles.map((rol) => (
            <Chip key={rol} label={rol} size="small" />
          ))}
        </Stack>
      ),
    },
    {
      key: 'activo',
      header: 'Activo',
      render: (u) => (
        <Switch
          checked={u.activo}
          onChange={() => toggleActivoMutation.mutate(u)}
          disabled={toggleActivoMutation.isPending}
        />
      ),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (u) => (
        <IconButton onClick={() => abrirEdicion(u)} size="small">
          <EditIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Usuarios</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nuevo usuario
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudieron cargar los usuarios.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(u) => u.id} />}

      <FormModal
        open={modalAlta}
        titulo="Nuevo usuario"
        onClose={() => setModalAlta(false)}
        onSubmit={altaForm.handleSubmit((values) => crearMutation.mutate(values))}
        submitting={crearMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="DNI"
            fullWidth
            error={!!altaForm.formState.errors.dni}
            helperText={altaForm.formState.errors.dni?.message}
            {...altaForm.register('dni')}
          />
          <TextField
            label="Email"
            fullWidth
            error={!!altaForm.formState.errors.email}
            helperText={altaForm.formState.errors.email?.message}
            {...altaForm.register('email')}
          />
          <TextField
            label="Nombre"
            fullWidth
            error={!!altaForm.formState.errors.nombre}
            helperText={altaForm.formState.errors.nombre?.message}
            {...altaForm.register('nombre')}
          />
          <TextField
            label="Apellido"
            fullWidth
            error={!!altaForm.formState.errors.apellido}
            helperText={altaForm.formState.errors.apellido?.message}
            {...altaForm.register('apellido')}
          />
          <TextField
            label="Contraseña"
            type="password"
            fullWidth
            error={!!altaForm.formState.errors.password}
            helperText={altaForm.formState.errors.password?.message}
            {...altaForm.register('password')}
          />
          <Controller
            name="roles"
            control={altaForm.control}
            render={({ field }) => <SelectorRoles value={field.value} onChange={field.onChange} />}
          />
          {altaForm.formState.errors.roles && (
            <Alert severity="error">{altaForm.formState.errors.roles.message}</Alert>
          )}
        </Stack>
      </FormModal>

      <FormModal
        open={!!usuarioEnEdicion}
        titulo="Editar usuario"
        onClose={() => setUsuarioEnEdicion(null)}
        onSubmit={edicionForm.handleSubmit((values) => {
          if (usuarioEnEdicion) editarMutation.mutate({ id: usuarioEnEdicion.id, values });
        })}
        submitting={editarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Email"
            fullWidth
            error={!!edicionForm.formState.errors.email}
            helperText={edicionForm.formState.errors.email?.message}
            {...edicionForm.register('email')}
          />
          <TextField
            label="Nombre"
            fullWidth
            error={!!edicionForm.formState.errors.nombre}
            helperText={edicionForm.formState.errors.nombre?.message}
            {...edicionForm.register('nombre')}
          />
          <TextField
            label="Apellido"
            fullWidth
            error={!!edicionForm.formState.errors.apellido}
            helperText={edicionForm.formState.errors.apellido?.message}
            {...edicionForm.register('apellido')}
          />
          <Controller
            name="roles"
            control={edicionForm.control}
            render={({ field }) => <SelectorRoles value={field.value} onChange={field.onChange} />}
          />
          {edicionForm.formState.errors.roles && (
            <Alert severity="error">{edicionForm.formState.errors.roles.message}</Alert>
          )}
        </Stack>
      </FormModal>
    </Stack>
  );
}
