import { useState } from 'react';
import { Button, type ButtonProps } from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import { httpClient } from '../api/httpClient';
import { useSnackbar } from './SnackbarProvider';

interface FileDownloadButtonProps extends Omit<ButtonProps, 'onClick'> {
  url: string;
  params?: Record<string, string | number | undefined>;
  nombreArchivo: string;
  label: string;
}

export function FileDownloadButton({ url, params, nombreArchivo, label, ...buttonProps }: FileDownloadButtonProps) {
  const { notify } = useSnackbar();
  const [descargando, setDescargando] = useState(false);

  const descargar = async () => {
    setDescargando(true);
    try {
      const response = await httpClient.get(url, { params, responseType: 'blob' });
      const blobUrl = URL.createObjectURL(response.data as Blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = nombreArchivo;
      link.click();
      URL.revokeObjectURL(blobUrl);
    } catch {
      notify('No se pudo generar la descarga', 'error');
    } finally {
      setDescargando(false);
    }
  };

  return (
    <Button startIcon={<DownloadIcon />} onClick={descargar} disabled={descargando} {...buttonProps}>
      {label}
    </Button>
  );
}
