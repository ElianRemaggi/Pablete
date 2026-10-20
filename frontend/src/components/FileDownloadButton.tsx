import { useState } from 'react';
import axios from 'axios';
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

/** Con responseType 'blob' el cuerpo de un error tambien llega como Blob: se lo lee para mostrar el mensaje
 * concreto del backend (permiso denegado, inscripcion inexistente...) en vez de un texto generico. */
async function mensajeDeError(error: unknown): Promise<string> {
  const fallback = 'No se pudo generar la descarga';
  if (!axios.isAxiosError(error)) return fallback;
  if (!error.response) return 'No hay conexión con el servidor. Reintentá en unos instantes.';
  const data = error.response.data;
  try {
    const texto = data instanceof Blob ? await data.text() : typeof data === 'string' ? data : '';
    const mensaje = (JSON.parse(texto) as { mensaje?: string }).mensaje;
    if (mensaje) return mensaje;
  } catch {
    // cuerpo no JSON: se cae al texto por estado
  }
  if (error.response.status === 403) return 'No tenés permiso para descargar este documento';
  if (error.response.status === 404) return 'El documento solicitado no existe';
  return fallback;
}

export function FileDownloadButton({ url, params, nombreArchivo, label, ...buttonProps }: FileDownloadButtonProps) {
  const { notify } = useSnackbar();
  const [descargando, setDescargando] = useState(false);

  const descargar = async () => {
    setDescargando(true);
    try {
      const response = await httpClient.get(url, { params, responseType: 'blob' });
      const blob = response.data as Blob;
      if (blob.size === 0) {
        notify('El servidor devolvió un archivo vacío', 'error');
        return;
      }
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = nombreArchivo;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(blobUrl);
    } catch (error) {
      notify(await mensajeDeError(error), 'error');
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
