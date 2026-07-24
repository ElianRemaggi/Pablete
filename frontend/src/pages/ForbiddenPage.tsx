import { Button, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export function ForbiddenPage() {
  return (
    <Container sx={{ mt: 12, textAlign: 'center' }}>
      <Typography variant="h3" gutterBottom>
        403
      </Typography>
      <Typography variant="body1" gutterBottom>
        No tenés permiso para acceder a esta página.
      </Typography>
      <Button component={Link} to="/login" variant="contained" sx={{ mt: 2 }}>
        Volver al inicio
      </Button>
    </Container>
  );
}
