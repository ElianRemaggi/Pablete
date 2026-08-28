import { useMemo, useState } from 'react';

export interface RangoMes {
  mes: Date;
  desde: Date;
  hasta: Date;
  cambiarMes: (delta: number) => void;
}

function inicioDeMes(fecha: Date): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), 1);
}

function finDeMes(fecha: Date): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth() + 1, 0);
}

/** Ventana de un mes calendario, con margen de una semana a cada lado para que la grilla de 6 filas quede completa. */
export function useMesCalendario(): RangoMes {
  const [mes, setMes] = useState(() => inicioDeMes(new Date()));

  const { desde, hasta } = useMemo(() => {
    const inicio = new Date(inicioDeMes(mes));
    inicio.setDate(inicio.getDate() - 7);
    const fin = new Date(finDeMes(mes));
    fin.setDate(fin.getDate() + 7);
    return { desde: inicio, hasta: fin };
  }, [mes]);

  const cambiarMes = (delta: number) => {
    setMes((actual) => new Date(actual.getFullYear(), actual.getMonth() + delta, 1));
  };

  return { mes, desde, hasta, cambiarMes };
}
