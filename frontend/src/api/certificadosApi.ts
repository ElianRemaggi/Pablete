import { httpClient } from './httpClient';

export interface CertificadoResponse {
  id: number;
  tipo: string;
  fechaEmision: string;
  urlDescarga: string;
}

export async function listarHistorialCertificados(): Promise<CertificadoResponse[]> {
  const { data } = await httpClient.get<CertificadoResponse[]>('/certificados/historial');
  return data;
}
