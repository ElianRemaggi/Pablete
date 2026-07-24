import { Button, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Container sx={{ mt: 12, textAlign: 'center' }}>
      <Typography variant="h3" gutterBottom>
        404
      </Typography>
      <Typography variant="body1" gutterBottom>
        La página que buscás no existe.
      </Typography>
      <Button component={Link} to="/" variant="contained" sx={{ mt: 2 }}>
        Volver al inicio
      </Button>
    </Container>
  );
}
