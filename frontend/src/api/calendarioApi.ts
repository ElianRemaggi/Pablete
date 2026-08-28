import { httpClient } from './httpClient';

export type TipoEventoCalendario = 'CURSADA' | 'FINAL' | 'INSCRIPCION' | 'EVENTO_ACADEMICO' | 'FECHA_LIMITE';

export interface EventoCalendarioResponse {
  id: string;
  titulo: string;
  detalle: string | null;
  inicio: string;
  fin: string | null;
  tipo: TipoEventoCalendario;
  ubicacion: string | null;
}

function formatearFecha(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

export async function obtenerCalendarioPropio(desde: Date, hasta: Date): Promise<EventoCalendarioResponse[]> {
  const { data } = await httpClient.get<EventoCalendarioResponse[]>('/calendario/me', {
    params: { desde: formatearFecha(desde), hasta: formatearFecha(hasta) },
  });
  return data;
}

export async function obtenerCalendarioInstitucional(desde: Date, hasta: Date): Promise<EventoCalendarioResponse[]> {
  const { data } = await httpClient.get<EventoCalendarioResponse[]>('/calendario/institucional', {
    params: { desde: formatearFecha(desde), hasta: formatearFecha(hasta) },
  });
  return data;
}

export async function obtenerCalendarioDeAlumno(
  alumnoId: number,
  desde: Date,
  hasta: Date,
): Promise<EventoCalendarioResponse[]> {
  const { data } = await httpClient.get<EventoCalendarioResponse[]>(`/calendario/alumno/${alumnoId}`, {
    params: { desde: formatearFecha(desde), hasta: formatearFecha(hasta) },
  });
  return data;
}

export interface EventoAcademicoResponse {
  id: number;
  titulo: string;
  descripcion: string | null;
  tipo: 'EVENTO_ACADEMICO' | 'FECHA_LIMITE';
  fechaInicio: string;
  fechaFin: string | null;
}

export interface EventoAcademicoRequest {
  titulo: string;
  descripcion?: string;
  tipo: 'EVENTO_ACADEMICO' | 'FECHA_LIMITE';
  fechaInicio: string;
  fechaFin?: string;
}

export async function listarEventosAcademicos(): Promise<EventoAcademicoResponse[]> {
  const { data } = await httpClient.get<EventoAcademicoResponse[]>('/eventos-academicos');
  return data;
}

export async function crearEventoAcademico(request: EventoAcademicoRequest): Promise<EventoAcademicoResponse> {
  const { data } = await httpClient.post<EventoAcademicoResponse>('/eventos-academicos', request);
  return data;
}

export async function actualizarEventoAcademico(
  id: number,
  request: EventoAcademicoRequest,
): Promise<EventoAcademicoResponse> {
  const { data } = await httpClient.put<EventoAcademicoResponse>(`/eventos-academicos/${id}`, request);
  return data;
}

export async function eliminarEventoAcademico(id: number): Promise<void> {
  await httpClient.delete(`/eventos-academicos/${id}`);
}
