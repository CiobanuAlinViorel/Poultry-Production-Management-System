import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#1B5E20' },
    secondary: { main: '#F9A825' },
    error: { main: '#C62828' },
    background: { default: '#f5f7f5' },
  },
  typography: { fontFamily: 'Inter, Roboto, Arial, sans-serif' },
  shape: { borderRadius: 10 },
  components: {
    MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { textTransform: 'none' } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
  },
});
