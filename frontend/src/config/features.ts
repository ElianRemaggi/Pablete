/**
 * Primera entrega: solo examenes finales. Las cursadas anuales, la inscripcion a materias, las comisiones,
 * los horarios y las correlatividades (que solo gobiernan la inscripcion a cursadas) quedan conservadas en el
 * codigo pero fuera del menu y de la navegacion. Para reactivarlas en la segunda etapa alcanza con definir
 * VITE_CURSADAS_HABILITADAS=true (por ejemplo en .env.development.local) y reiniciar Vite; no hay que tocar codigo.
 * El backend conserva sus endpoints: esta bandera controla la interfaz de usuario.
 */
export const CURSADAS_HABILITADAS = import.meta.env.VITE_CURSADAS_HABILITADAS === 'true';
