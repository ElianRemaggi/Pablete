import { Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export function PreceptorDashboardPage() {
  return (
    <Stack spacing={3}>
      <Typography variant="h4">Panel del Preceptor</Typography>
      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography>
              Desde acá podés buscar alumnos por legajo, DNI o apellido, ver su ficha con estado académico, y
              generar exportaciones en Excel de inscriptos por comisión o mesa.
            </Typography>
            <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
              <Button component={Link} to="/preceptor/busqueda-alumnos" variant="contained">
                Buscar alumnos
              </Button>
              <Button component={Link} to="/preceptor/exportaciones" variant="outlined">
                Exportaciones
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
