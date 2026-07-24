import { Alert, Typography } from '@mui/material';

interface StubPageProps {
  titulo: string;
  detalle?: string;
}

export function StubPage({ titulo, detalle }: StubPageProps) {
  return (
    <>
      <Typography variant="h4" gutterBottom>
        {titulo}
      </Typography>
      <Alert severity="info">{detalle ?? 'Esta pantalla todavía no está implementada.'}</Alert>
    </>
  );
}
