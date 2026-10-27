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

export interface CambioImportacion {
  hoja: string;
  dni: string;
  alumno: string;
  materia: string;
  tipo: 'CREAR' | 'ACTUALIZAR' | 'CONFLICTO';
  estadoActual: string | null;
  estadoNuevo: string | null;
  notaActual: number | null;
  notaNueva: number | null;
  detalle: string | null;
}

export interface ErrorImportacion {
  hoja: string;
  dni: string;
  materia: string | null;
  motivo: string;
}

export interface ImportacionHistorialResponse {
  confirmada: boolean;
  archivo: string;
  hojas: number;
  filasLeidas: number;
  alumnosExistentes: number;
  alumnosNuevos: number;
  registrosCreados: number;
  registrosActualizados: number;
  registrosSinCambios: number;
  registrosEnConflicto: number;
  registrosRechazados: number;
  filasRechazadas: number;
  cambios: CambioImportacion[];
  hayMasCambios: boolean;
  errores: ErrorImportacion[];
  advertencias: string[];
}

/** confirmar=false: solo valida y devuelve la vista previa (no modifica datos); confirmar=true: aplica. */
export async function importarHistorial(
  archivo: File,
  confirmar: boolean,
  crearAlumnos: boolean,
): Promise<ImportacionHistorialResponse> {
  const formData = new FormData();
  formData.append('archivo', archivo);
  const { data } = await httpClient.post<ImportacionHistorialResponse>('/historial-academico/importar', formData, {
    params: { confirmar, crearAlumnos },
    timeout: 300000,
  });
  return data;
}
