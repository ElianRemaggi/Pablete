import { httpClient } from './httpClient';

export interface AlumnoResponse {
  usuarioId: number;
  dni: string;
  nombre: string;
  apellido: string;
  email: string;
  legajo: string;
  fechaIngreso: string;
}

export interface HistorialAcademicoResponse {
  materiaCodigo: string;
  materiaNombre: string;
  estado: string;
  fechaActualizacion: string;
}

export async function obtenerAlumnoPropio(): Promise<AlumnoResponse> {
  const { data } = await httpClient.get<AlumnoResponse>('/alumnos/me');
  return data;
}

export async function obtenerHistorialPropio(): Promise<HistorialAcademicoResponse[]> {
  const { data } = await httpClient.get<HistorialAcademicoResponse[]>('/alumnos/me/historial-academico');
  return data;
}

export async function obtenerMateriasAprobadas(): Promise<HistorialAcademicoResponse[]> {
  const { data } = await httpClient.get<HistorialAcademicoResponse[]>('/alumnos/me/materias-aprobadas');
  return data;
}

export async function obtenerMateriasPendientes(): Promise<HistorialAcademicoResponse[]> {
  const { data } = await httpClient.get<HistorialAcademicoResponse[]>('/alumnos/me/materias-pendientes');
  return data;
}

export interface FiltrosBusquedaAlumno {
  legajo?: string;
  dni?: string;
  apellido?: string;
  nombre?: string;
}

export async function buscarAlumnos(filtros: FiltrosBusquedaAlumno): Promise<AlumnoResponse[]> {
  const params: Record<string, string> = {};
  if (filtros.legajo) params.legajo = filtros.legajo;
  if (filtros.dni) params.dni = filtros.dni;
  if (filtros.apellido) params.apellido = filtros.apellido;
  if (filtros.nombre) params.nombre = filtros.nombre;

  const { data } = await httpClient.get<AlumnoResponse[]>('/alumnos', { params });
  return data;
}

export async function obtenerAlumnoPorId(id: number): Promise<AlumnoResponse> {
  const { data } = await httpClient.get<AlumnoResponse>(`/alumnos/${id}`);
  return data;
}

export async function obtenerHistorialPorId(id: number): Promise<HistorialAcademicoResponse[]> {
  const { data } = await httpClient.get<HistorialAcademicoResponse[]>(`/alumnos/${id}/historial-academico`);
  return data;
}
