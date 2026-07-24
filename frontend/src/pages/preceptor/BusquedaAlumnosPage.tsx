import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, Stack, TextField, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { buscarAlumnos, type AlumnoResponse } from '../../api/alumnosApi';

export function BusquedaAlumnosPage() {
  const [legajo, setLegajo] = useState('');
  const [dni, setDni] = useState('');
  const [apellido, setApellido] = useState('');
  const navigate = useNavigate();

  const filtros = useDebouncedValue({ legajo, dni, apellido });
  const hayFiltro = !!(filtros.legajo || filtros.dni || filtros.apellido);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['busqueda-alumnos', filtros],
    queryFn: () => buscarAlumnos(filtros),
    enabled: hayFiltro,
  });

  const columnas: DataTableColumn<AlumnoResponse>[] = [
    { key: 'legajo', header: 'Legajo', render: (a) => a.legajo },
    { key: 'dni', header: 'DNI', render: (a) => a.dni },
    { key: 'nombre', header: 'Nombre', render: (a) => `${a.nombre} ${a.apellido}` },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (a) => (
        <Button size="small" onClick={() => navigate(`/preceptor/alumno/${a.usuarioId}`)}>
          Ver ficha
        </Button>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Búsqueda de alumnos</Typography>

      <Stack direction="row" spacing={2}>
        <TextField label="Legajo" value={legajo} onChange={(e) => setLegajo(e.target.value)} size="small" />
        <TextField label="DNI" value={dni} onChange={(e) => setDni(e.target.value)} size="small" />
        <TextField label="Apellido" value={apellido} onChange={(e) => setApellido(e.target.value)} size="small" />
      </Stack>

      {!hayFiltro && <Alert severity="info">Ingresá al menos un criterio de búsqueda.</Alert>}
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo realizar la búsqueda.</Alert>}
      {data && data.length === 0 && <Alert severity="info">No se encontraron alumnos.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(a) => a.usuarioId} />}
    </Stack>
  );
}
