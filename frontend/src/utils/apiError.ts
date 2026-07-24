import axios from 'axios';

export function extraerMensajeError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && error.response?.data && typeof error.response.data === 'object') {
    const data = error.response.data as { mensaje?: string };
    if (data.mensaje) return data.mensaje;
  }
  return fallback;
}
