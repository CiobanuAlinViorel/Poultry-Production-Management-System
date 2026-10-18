import { AdminPanelSettings, Agriculture, Egg, Factory, Home, Logout, Person, Security, Settings, Visibility, Warehouse } from '@mui/icons-material';
import { Alert, AppBar, BottomNavigation, BottomNavigationAction, Box, Button, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, ListSubheader, MenuItem, TextField, Toolbar, Tooltip, Typography, useMediaQuery, useTheme } from '@mui/material';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { apiClient } from '../shared/api/client';
import { useRoles } from '../shared/api/hooks';
import { ModuleRoles, Roles } from '../shared/auth/access';
import { useAccess, useAuthStore } from '../shared/auth/store';

const drawerWidth = 260;

export function AppShell() {
  const theme = useTheme();
  const mobile = useMediaQuery(theme.breakpoints.down('md'));
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const clearSession = useAuthStore((state) => state.clearSession);
  const setViewAsRole = useAuthStore((state) => state.setViewAsRole);
  const user = useAuthStore((state) => state.user);
  const { canRead, realIsSystemAdmin, viewAsRole } = useAccess();
  const rolesQuery = useRoles(realIsSystemAdmin);
  const previewRoles = rolesQuery.data?.filter((r) => r.active).map((r) => ({ code: r.code, name: i18n.language.startsWith('ro') ? r.nameRo : (r.nameEn ?? r.nameRo) }))
    ?? Object.values(Roles).map((code) => ({ code, name: code }));

  interface NavItem { path: string; label: string; icon: ReactNode; roles?: readonly string[] }
  interface NavSection { header?: string; items: NavItem[] }
  const allSections: NavSection[] = [
    { items: [{ path: '/', label: t('dashboard'), icon: <Home /> }] },
    {
      header: t('nav.operations'),
      items: [
        { path: '/parent-flock', label: t('nav.parentFlock'), icon: <Egg />, roles: ModuleRoles.parentFlock },
        { path: '/hatchery', label: t('nav.hatchery'), icon: <Warehouse />, roles: ModuleRoles.hatchery },
        { path: '/farm', label: t('farm.dashboard.title'), icon: <Agriculture />, roles: ModuleRoles.farm },
        { path: '/slaughterhouse', label: t('nav.slaughterhouse'), icon: <Factory />, roles: ModuleRoles.slaughterhouse },
      ],
    },
    {
      header: t('nav.administration'),
      items: [
        { path: '/admin/users', label: t('users'), icon: <AdminPanelSettings />, roles: ModuleRoles.admin },
        { path: '/admin/roles', label: t('nav.roles'), icon: <Security />, roles: ModuleRoles.admin },
        { path: '/admin/settings', label: t('settings'), icon: <Settings />, roles: ModuleRoles.admin },
      ],
    },
    { items: [{ path: '/profile', label: t('profile'), icon: <Person /> }] },
  ];
  const sections = allSections.map((s) => ({ ...s, items: s.items.filter((item) => !item.roles || canRead(item.roles)) })).filter((s) => s.items.length > 0);
  const flatItems = sections.flatMap((s) => s.items);
  const isSelected = (path: string) => (path === '/' ? location.pathname === '/' : location.pathname.startsWith(path));

  const logout = async () => {
    try { await apiClient.post('/api/identity/auth/logout'); } finally { clearSession(); navigate('/login', { replace: true }); }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: theme.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 1 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>{t('appName')}</Typography>
          {realIsSystemAdmin && (
            <Tooltip title={t('viewAs.tooltip')}>
              <TextField select size="small" value={viewAsRole ?? ''} onChange={(e) => setViewAsRole(e.target.value || null)}
                sx={{ minWidth: 220, '& .MuiInputBase-root': { color: 'inherit' }, '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.5)' }, '& .MuiSvgIcon-root': { color: 'inherit' } }}
                slotProps={{ input: { startAdornment: <Visibility fontSize="small" sx={{ mr: 1 }} /> } }}>
                <MenuItem value="">{t('viewAs.own')}</MenuItem>
                {previewRoles.filter((r) => r.code !== Roles.SYSTEM_ADMIN).map((r) => <MenuItem key={r.code} value={r.code}>{r.name}</MenuItem>)}
              </TextField>
            </Tooltip>
          )}
          {!mobile && <Typography variant="body2" sx={{ opacity: 0.85 }}>{user?.displayName ?? user?.email}</Typography>}
          <Button color="inherit" onClick={() => void i18n.changeLanguage(i18n.language.startsWith('ro') ? 'en' : 'ro')}>{i18n.language.startsWith('ro') ? 'EN' : 'RO'}</Button>
          <IconButton color="inherit" aria-label={t('logout')} onClick={() => void logout()}><Logout /></IconButton>
        </Toolbar>
      </AppBar>
      {!mobile && (
        <Drawer variant="permanent" sx={{ width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }}>
          <Toolbar />
          <Box sx={{ overflow: 'auto' }}>
            {sections.map((section, i) => (
              <Box key={i}>
                {i > 0 && <Divider />}
                <List dense subheader={section.header ? <ListSubheader disableSticky>{section.header}</ListSubheader> : undefined}>
                  {section.items.map((item) => (
                    <ListItemButton key={item.path} selected={isSelected(item.path)} onClick={() => navigate(item.path)}>
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.label} />
                    </ListItemButton>
                  ))}
                </List>
              </Box>
            ))}
          </Box>
        </Drawer>
      )}
      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, sm: 3 }, mt: 8, mb: mobile ? 7 : 0, minWidth: 0 }}>
        {viewAsRole && (
          <Alert severity="info" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => setViewAsRole(null)}>{t('viewAs.exit')}</Button>}>
            {t('viewAs.banner', { role: previewRoles.find((r) => r.code === viewAsRole)?.name ?? viewAsRole })}
          </Alert>
        )}
        <Outlet />
      </Box>
      {mobile && (
        <BottomNavigation showLabels value={flatItems.findIndex((item) => isSelected(item.path))} onChange={(_, value: number) => navigate(flatItems[value]?.path ?? '/')}
          sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: theme.zIndex.appBar, overflowX: 'auto' }}>
          {flatItems.map((item) => <BottomNavigationAction key={item.path} label={item.label} icon={item.icon} />)}
        </BottomNavigation>
      )}
    </Box>
  );
}
