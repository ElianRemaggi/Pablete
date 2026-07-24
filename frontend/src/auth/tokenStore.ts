import type { AuthState } from '../types/auth';

/**
 * Fuente de verdad para el token actual, fuera de React — el interceptor de Axios
 * corre fuera del ciclo de render y no puede leer contexto. AuthContext se suscribe
 * a este store (useSyncExternalStore) para que React re-renderice en cada cambio.
 */
type Listener = (auth: AuthState | null) => void;

let currentAuth: AuthState | null = null;
const listeners = new Set<Listener>();

export function getAuth(): AuthState | null {
  return currentAuth;
}

export function setAuth(auth: AuthState | null): void {
  currentAuth = auth;
  listeners.forEach((listener) => listener(auth));
}

export function subscribe(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
