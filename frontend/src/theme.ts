import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#0B3D91' },
    secondary: { main: '#F2A900' },
    background: { default: '#F5F6F8' },
  },
  typography: { fontFamily: 'Inter, Roboto, sans-serif' },
  shape: { borderRadius: 8 },
});
