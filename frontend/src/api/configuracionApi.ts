import { httpClient } from './httpClient';

export interface ConfiguracionResponse {
  fechaAperturaCursadas: string;
  fechaCierreCursadas: string;
  fechaAperturaFinales: string;
  fechaCierreFinales: string;
  limiteIntentosFinal: number;
  horasBloqueoFinal: number;
}

export async function obtenerConfiguracion(): Promise<ConfiguracionResponse> {
  const { data } = await httpClient.get<ConfiguracionResponse>('/configuracion');
  return data;
}

export async function actualizarConfiguracion(request: ConfiguracionResponse): Promise<ConfiguracionResponse> {
  const { data } = await httpClient.put<ConfiguracionResponse>('/configuracion', request);
  return data;
}
