import { httpClient } from './httpClient';

export interface HistorialImportResultResponse {
  hojasProcesadas: number;
  filasLeidas: number;
  actualizados: number;
  alumnosNoEncontrados: number;
  materiasNoEncontradas: number;
  fecha: string;
  advertencias: string[];
}

export async function recargarHistorialDesdeExcel(): Promise<HistorialImportResultResponse> {
  const { data } = await httpClient.post<HistorialImportResultResponse>('/historial-academico/recargar');
  return data;
}
