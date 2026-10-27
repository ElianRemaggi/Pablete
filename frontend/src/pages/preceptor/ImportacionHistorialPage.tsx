import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Alert,
  AlertTitle,
  Button,
  Card,
  CardContent,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Stack,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import {
  importarHistorial,
  type CambioImportacion,
  type ErrorImportacion,
  type ImportacionHistorialResponse,
} from '../../api/historialAcademicoApi';

function csv(valor: string | number | null | undefined): string {
  const texto = valor == null ? '' : String(valor);
  return `"${texto.replace(/"/g, '""')}"`;
}

function descargarReporteErrores(errores: ErrorImportacion[], archivo: string) {
  const lineas = [['Hoja', 'DNI', 'Materia', 'Motivo'].map(csv).join(';')];
  for (const e of errores) lineas.push([e.hoja, e.dni, e.materia, e.motivo].map(csv).join(';'));
  // BOM para que Excel abra bien los acentos.
  const blob = new Blob(['﻿' + lineas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `errores-importacion-${archivo.replace(/\.xlsx$/i, '')}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

const columnasCambios: DataTableColumn<CambioImportacion>[] = [
  { key: 'tipo', header: 'Acción', render: (c) => c.tipo },
  { key: 'alumno', header: 'Alumno', render: (c) => `${c.alumno} (${c.dni})` },
  { key: 'materia', header: 'Materia', render: (c) => c.materia },
  { key: 'actual', header: 'En SIGA', render: (c) => (c.estadoActual ? `${c.estadoActual}${c.notaActual != null ? ` (${c.notaActual})` : ''}` : '—') },
  { key: 'nuevo', header: 'En el Excel', render: (c) => `${c.estadoNuevo ?? '—'}${c.notaNueva != null ? ` (${c.notaNueva})` : ''}` },
  { key: 'detalle', header: 'Detalle', render: (c) => c.detalle ?? '' },
];

const columnasErrores: DataTableColumn<ErrorImportacion>[] = [
  { key: 'hoja', header: 'Hoja', render: (e) => e.hoja },
  { key: 'dni', header: 'DNI', render: (e) => e.dni },
  { key: 'materia', header: 'Materia', render: (e) => e.materia ?? '—' },
  { key: 'motivo', header: 'Motivo', render: (e) => e.motivo },
];

/** Importacion masiva del historial academico desde un .xlsx (preceptor y administrador). Siempre se valida primero
 * (vista previa, sin tocar datos) y recien despues se confirma; el backend aplica exactamente lo que mostro la vista previa. */
export function ImportacionHistorialPage() {
  const { notify } = useSnackbar();
  const inputRef = useRef<HTMLInputElement>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [crearAlumnos, setCrearAlumnos] = useState(false);
  const [previa, setPrevia] = useState<ImportacionHistorialResponse | null>(null);
  const [resultado, setResultado] = useState<ImportacionHistorialResponse | null>(null);
  const [confirmando, setConfirmando] = useState(false);

  const validar = useMutation({
    mutationFn: () => importarHistorial(archivo as File, false, crearAlumnos),
    onSuccess: (r) => {
      setPrevia(r);
      setResultado(null);
    },
    onError: (err) => {
      setPrevia(null);
      notify(extraerMensajeError(err, 'No se pudo validar el archivo'), 'error');
    },
  });

  const ejecutar = useMutation({
    mutationFn: () => importarHistorial(archivo as File, true, crearAlumnos),
    onSuccess: (r) => {
      setResultado(r);
      setPrevia(null);
      setConfirmando(false);
      notify('Importación realizada', 'success');
    },
    onError: (err) => {
      setConfirmando(false);
      notify(extraerMensajeError(err, 'No se pudo realizar la importación'), 'error');
    },
  });

  const elegirArchivo = (f: File | null) => {
    setArchivo(f);
    setPrevia(null);
    setResultado(null);
  };

  const mostrado = resultado ?? previa;
  const hayAlgoParaAplicar =
    !!previa && previa.registrosCreados + previa.registrosActualizados + previa.alumnosNuevos > 0;

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Importar historial académico</Typography>
      <Typography color="text.secondary">
        Subí el libro de Excel con los alumnos y su historial (una hoja por carrera/plan; encabezados en la fila 1,
        alumnos desde la fila 3). Primero se valida y se muestra qué cambiaría; recién al confirmar se modifican los datos.
        Lo que ya está en SIGA no se pisa: el Excel solo completa o avanza, y las diferencias se informan como conflictos.
      </Typography>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
        <input
          ref={inputRef}
          type="file"
          accept=".xlsx"
          hidden
          onChange={(e) => elegirArchivo(e.target.files?.[0] ?? null)}
        />
        <Button variant="outlined" startIcon={<UploadFileIcon />} onClick={() => inputRef.current?.click()}>
          Elegir archivo .xlsx
        </Button>
        <Typography>{archivo ? `${archivo.name} (${Math.round(archivo.size / 1024)} KB)` : 'Ningún archivo seleccionado'}</Typography>
        <FileDownloadButton
          url="/historial-academico/plantilla"
          nombreArchivo="historial-academico-plantilla.xlsx"
          label="Descargar plantilla"
          variant="text"
        />
      </Stack>

      <FormControlLabel
        control={<Checkbox checked={crearAlumnos} onChange={(e) => { setCrearAlumnos(e.target.checked); setPrevia(null); }} />}
        label="Crear los alumnos que no existan en SIGA"
      />
      {crearAlumnos && (
        <Alert severity="warning">
          Los alumnos nuevos se crean con su DNI como usuario <strong>y como contraseña inicial</strong>, legajo igual al DNI y
          un email provisorio (<em>dni@sin-email.siga.local</em>). Deberían cambiar la clave y cargar su email real.
          No se asigna carrera: SIGA no la guarda por alumno.
        </Alert>
      )}

      <Stack direction="row" spacing={2}>
        <Button variant="contained" disabled={!archivo || validar.isPending} onClick={() => validar.mutate()}>
          Validar y ver vista previa
        </Button>
        {validar.isPending && <CircularProgress size={24} />}
      </Stack>

      {mostrado && (
        <Card>
          <CardContent>
            <Stack spacing={1}>
              <Typography variant="h6">
                {mostrado.confirmada ? 'Importación realizada' : 'Vista previa (todavía no se modificó nada)'} — {mostrado.archivo}
              </Typography>
              <Typography>
                {mostrado.hojas} hoja(s), {mostrado.filasLeidas} fila(s) de alumnos ({mostrado.alumnosExistentes} existentes
                {mostrado.alumnosNuevos > 0 ? `, ${mostrado.alumnosNuevos} nuevos` : ''}).
              </Typography>
              <Typography>
                Registros: <strong>{mostrado.registrosCreados}</strong> a crear · <strong>{mostrado.registrosActualizados}</strong> a
                actualizar · <strong>{mostrado.registrosSinCambios}</strong> sin cambios ·{' '}
                <strong>{mostrado.registrosEnConflicto}</strong> en conflicto (se conserva SIGA) ·{' '}
                <strong>{mostrado.registrosRechazados + mostrado.filasRechazadas}</strong> rechazados.
              </Typography>
              {mostrado.confirmada && (
                <Alert severity="success">Los datos ya están disponibles para cursadas, finales, correlatividades e historial.</Alert>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {mostrado && mostrado.errores.length > 0 && (
        <Stack spacing={1}>
          <Alert
            severity="error"
            action={
              <Button color="inherit" size="small" onClick={() => descargarReporteErrores(mostrado.errores, mostrado.archivo)}>
                Descargar reporte de errores
              </Button>
            }
          >
            <AlertTitle>{mostrado.errores.length} fila(s) o celda(s) rechazadas</AlertTitle>
            No frenan el resto de la importación; corregilas en el Excel y volvé a subirlo (lo ya importado no se duplica).
          </Alert>
          <DataTable columns={columnasErrores} rows={mostrado.errores.slice(0, 200)} getRowKey={(e) => `${e.hoja}|${e.dni}|${e.materia}|${e.motivo}`} />
          {mostrado.errores.length > 200 && (
            <Typography variant="caption">Se muestran 200; el reporte descargable incluye todos.</Typography>
          )}
        </Stack>
      )}

      {mostrado && mostrado.advertencias.length > 0 && (
        <Alert severity="warning">
          <AlertTitle>Advertencias</AlertTitle>
          {mostrado.advertencias.slice(0, 20).map((a) => (
            <div key={a}>{a}</div>
          ))}
        </Alert>
      )}

      {mostrado && mostrado.cambios.length > 0 && (
        <Stack spacing={1}>
          <Typography variant="h6">Cambios {mostrado.confirmada ? 'aplicados' : 'previstos'}</Typography>
          <DataTable
            columns={columnasCambios}
            rows={mostrado.cambios}
            getRowKey={(c) => `${c.hoja}|${c.dni}|${c.materia}|${c.tipo}`}
          />
          {mostrado.hayMasCambios && <Typography variant="caption">Se listan los primeros 500 cambios.</Typography>}
        </Stack>
      )}

      {previa && (
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Button variant="contained" color="success" disabled={!hayAlgoParaAplicar || ejecutar.isPending} onClick={() => setConfirmando(true)}>
            Confirmar importación
          </Button>
          {!hayAlgoParaAplicar && <Typography color="text.secondary">No hay nada para aplicar.</Typography>}
        </Stack>
      )}

      <ConfirmDialog
        open={confirmando}
        titulo="Confirmar importación"
        mensaje={
          previa
            ? `Se van a crear ${previa.registrosCreados} registros, actualizar ${previa.registrosActualizados}` +
              (previa.alumnosNuevos ? ` y dar de alta ${previa.alumnosNuevos} alumnos` : '') +
              '. Los conflictos y rechazados no se aplican. ¿Continuar?'
            : ''
        }
        onConfirm={() => ejecutar.mutate()}
        onCancel={() => setConfirmando(false)}
      />
    </Stack>
  );
}
