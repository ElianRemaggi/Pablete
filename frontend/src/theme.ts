import { createTheme } from '@mui/material/styles';

export const COLOR_PRIMARIO = '#05acec';
export const COLOR_PRIMARIO_CLARO = '#5fd0ff';
export const COLOR_PRIMARIO_OSCURO = '#0378a6';
export const COLOR_SECUNDARIO = '#0f3b5c';

export const theme = createTheme({
  palette: {
    primary: {
      main: COLOR_PRIMARIO,
      light: COLOR_PRIMARIO_CLARO,
      dark: COLOR_PRIMARIO_OSCURO,
      contrastText: '#ffffff',
    },
    secondary: { main: COLOR_SECUNDARIO },
    background: { default: '#f4f9fc' },
  },
  typography: { fontFamily: 'Inter, Roboto, sans-serif' },
  shape: { borderRadius: 8 },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: COLOR_PRIMARIO,
          color: '#ffffff',
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          '&.Mui-selected': {
            backgroundColor: 'rgba(5, 172, 236, 0.14)',
            borderRight: `3px solid ${COLOR_PRIMARIO}`,
            '&:hover': {
              backgroundColor: 'rgba(5, 172, 236, 0.2)',
            },
          },
          '&:hover': {
            backgroundColor: 'rgba(5, 172, 236, 0.08)',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid rgba(15, 59, 92, 0.08)',
        },
      },
    },
  },
});
