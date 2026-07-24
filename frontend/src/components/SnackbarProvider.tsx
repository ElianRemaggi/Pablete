import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { Alert, Snackbar, type AlertColor } from '@mui/material';

interface SnackbarContextValue {
  notify: (mensaje: string, severity?: AlertColor) => void;
}

const SnackbarContext = createContext<SnackbarContextValue | undefined>(undefined);

export function SnackbarProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ open: boolean; mensaje: string; severity: AlertColor }>({
    open: false,
    mensaje: '',
    severity: 'info',
  });

  const notify = useCallback((mensaje: string, severity: AlertColor = 'info') => {
    setState({ open: true, mensaje, severity });
  }, []);

  const handleClose = () => setState((prev) => ({ ...prev, open: false }));

  return (
    <SnackbarContext.Provider value={{ notify }}>
      {children}
      <Snackbar
        open={state.open}
        autoHideDuration={4000}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleClose} severity={state.severity} sx={{ width: '100%' }}>
          {state.mensaje}
        </Alert>
      </Snackbar>
    </SnackbarContext.Provider>
  );
}

export function useSnackbar(): SnackbarContextValue {
  const context = useContext(SnackbarContext);
  if (!context) {
    throw new Error('useSnackbar debe usarse dentro de SnackbarProvider');
  }
  return context;
}
