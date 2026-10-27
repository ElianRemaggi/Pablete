import { useEffect, useState } from 'react';
import { Alert, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { FormModal } from './FormModal';

export interface OpcionResultado {
  value: string;
  label: string;
  /** true: el backend exige una nota 1-10; false: no admite nota. */
  conNota: boolean;
}

interface CalificacionDialogProps {
  open: boolean;
  titulo: string;
  descripcion: string;
  opciones: OpcionResultado[];
  resultadoInicial: string;
  notaInicial: number | null;
  submitting?: boolean;
  onClose: () => void;
  onSubmit: (resultado: string, nota: number | null) => void;
}

/** Formulario de nota + resultado compartido por la acreditacion de cursadas y de finales. El backend vuelve a
 * validar todo (nota 1-10 obligatoria segun el resultado); aca solo se evita mandar combinaciones inconsistentes. */
export function CalificacionDialog({
  open,
  titulo,
  descripcion,
  opciones,
  resultadoInicial,
  notaInicial,
  submitting,
  onClose,
  onSubmit,
}: CalificacionDialogProps) {
  const [resultado, setResultado] = useState(resultadoInicial);
  const [nota, setNota] = useState(notaInicial?.toString() ?? '');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setResultado(resultadoInicial);
      setNota(notaInicial?.toString() ?? '');
      setError(null);
    }
  }, [open, resultadoInicial, notaInicial]);

  const conNota = opciones.find((o) => o.value === resultado)?.conNota ?? false;

  const confirmar = () => {
    if (!conNota) {
      onSubmit(resultado, null);
      return;
    }
    const valor = Number(nota);
    if (nota.trim() === '' || !Number.isInteger(valor) || valor < 1 || valor > 10) {
      setError('Ingresá una nota entera entre 1 y 10.');
      return;
    }
    onSubmit(resultado, valor);
  };

  return (
    <FormModal open={open} titulo={titulo} onClose={onClose} onSubmit={confirmar} submitting={submitting}>
      <Stack spacing={2} sx={{ pt: 1 }}>
        <Typography color="text.secondary">{descripcion}</Typography>
        <TextField select label="Resultado" value={resultado} onChange={(e) => setResultado(e.target.value)}>
          {opciones.map((o) => (
            <MenuItem key={o.value} value={o.value}>
              {o.label}
            </MenuItem>
          ))}
        </TextField>
        {conNota && (
          <TextField
            label="Nota (1 a 10)"
            type="number"
            value={nota}
            onChange={(e) => setNota(e.target.value)}
            slotProps={{ htmlInput: { min: 1, max: 10, step: 1 } }}
          />
        )}
        {error && <Alert severity="warning">{error}</Alert>}
      </Stack>
    </FormModal>
  );
}
