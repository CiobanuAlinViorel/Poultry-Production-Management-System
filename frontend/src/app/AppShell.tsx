import { AdminPanelSettings, Home, Logout, Person, Settings } from '@mui/icons-material';
import { AppBar, BottomNavigation, BottomNavigationAction, Box, Button, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography, useMediaQuery, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { apiClient } from '../shared/api/client';
import { useAuthStore } from '../shared/auth/store';

const drawerWidth = 240;
export function AppShell() {
  const theme = useTheme();
  const mobile = useMediaQuery(theme.breakpoints.down('md'));
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const clearSession = useAuthStore((state) => state.clearSession);
  const items = [
    { path: '/', label: t('dashboard'), icon: <Home /> },
    { path: '/admin/users', label: t('users'), icon: <AdminPanelSettings /> },
    { path: '/admin/settings', label: t('settings'), icon: <Settings /> },
    { path: '/profile', label: t('profile'), icon: <Person /> },
  ];
  const logout = async () => {
    try { await apiClient.post('/api/identity/auth/logout'); } finally { clearSession(); navigate('/login', { replace: true }); }
  };
  return <Box sx={{ display: 'flex', minHeight: '100vh' }}><AppBar position="fixed" sx={{ zIndex: theme.zIndex.drawer + 1 }}><Toolbar><Typography variant="h6" sx={{ flexGrow: 1 }}>{t('appName')}</Typography><Button color="inherit" onClick={() => void i18n.changeLanguage(i18n.language.startsWith('ro') ? 'en' : 'ro')}>{i18n.language.startsWith('ro') ? 'EN' : 'RO'}</Button><IconButton color="inherit" aria-label={t('logout')} onClick={() => void logout()}><Logout /></IconButton></Toolbar></AppBar>{!mobile && <Drawer variant="permanent" sx={{ width: drawerWidth, flexShrink: 0, [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' } }}><Toolbar /><Box sx={{ overflow: 'auto' }}><List>{items.map((item) => <ListItemButton key={item.path} selected={location.pathname === item.path} onClick={() => navigate(item.path)}><ListItemIcon>{item.icon}</ListItemIcon><ListItemText primary={item.label} /></ListItemButton>)}</List><Divider /></Box></Drawer>}<Box component="main" sx={{ flexGrow: 1, p: { xs: 2, sm: 3 }, mt: 8, mb: mobile ? 7 : 0, minWidth: 0 }}><Outlet /></Box>{mobile && <BottomNavigation showLabels value={items.findIndex((item) => item.path === location.pathname)} onChange={(_, value: number) => navigate(items[value]?.path ?? '/')} sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: theme.zIndex.appBar }}>
    {items.map((item) => <BottomNavigationAction key={item.path} label={item.label} icon={item.icon} />)}
  </BottomNavigation>}</Box>;
}
