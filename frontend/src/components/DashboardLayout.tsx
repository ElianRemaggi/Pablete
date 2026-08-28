import { useState, type ReactNode } from 'react';
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { logout as logoutRequest } from '../api/authApi';
import logoIset from '../assets/logo-iset.jpg';

const DRAWER_WIDTH = 260;

export interface NavItem {
  label: string;
  to: string;
  icon: ReactNode;
}

interface DashboardLayoutProps {
  titulo: string;
  navItems: NavItem[];
}

export function DashboardLayout({ titulo, navItems }: DashboardLayoutProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const { auth, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    if (auth) {
      try {
        await logoutRequest(auth.refreshToken);
      } catch {
        // best-effort: si el logout remoto falla igual limpiamos la sesion local
      }
    }
    logout();
    navigate('/login', { replace: true });
  };

  const drawerContent = (
    <div>
      <Toolbar sx={{ gap: 1.5 }}>
        <Box
          component="img"
          src={logoIset}
          alt="ISET"
          sx={{ height: 36, width: 36, objectFit: 'contain' }}
        />
        <Typography variant="h6" noWrap component="div">
          SIGA
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {navItems.map((item) => (
          <ListItem key={item.to} disablePadding>
            <ListItemButton
              component={Link}
              to={item.to}
              selected={location.pathname === item.to}
              onClick={() => setMobileOpen(false)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          {isMobile && (
            <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 2 }}>
              <MenuIcon />
            </IconButton>
          )}
          {isMobile && (
            <Box
              component="img"
              src={logoIset}
              alt="ISET"
              sx={{ height: 28, width: 28, objectFit: 'contain', mr: 1.5, borderRadius: '4px', bgcolor: '#fff' }}
            />
          )}
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            {titulo}
          </Typography>
          <Typography variant="body2" sx={{ mr: 2 }}>
            {auth?.nombre}
          </Typography>
          <IconButton color="inherit" onClick={handleLogout} title="Cerrar sesión">
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Box component="nav" sx={{ width: { sm: DRAWER_WIDTH }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant={isMobile ? 'temporary' : 'permanent'}
          open={isMobile ? mobileOpen : true}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{ '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH } }}
        >
          {drawerContent}
        </Drawer>
      </Box>
      <Box component="main" sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` } }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
