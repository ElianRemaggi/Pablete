/**
 * Inscripcion a cursadas, comisiones, horarios y correlatividades. Desde la entrega de acreditacion de cursadas
 * estan ACTIVAS por defecto (el flujo inscripcion -> nota de cursada -> habilitacion del final depende de ellas).
 * Para ocultarlas de la interfaz definir VITE_CURSADAS_HABILITADAS=false (por ejemplo en .env.development.local) y
 * reiniciar Vite. El backend conserva sus endpoints en cualquier caso: esta bandera controla la interfaz.
 */
export const CURSADAS_HABILITADAS = import.meta.env.VITE_CURSADAS_HABILITADAS !== 'false';
