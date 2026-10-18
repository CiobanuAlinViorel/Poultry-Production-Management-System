import type { ReactNode } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import { useAccess, useAuthStore } from '../shared/auth/store';
import { ModuleRoles } from '../shared/auth/access';
import { LoginPage } from '../modules/identity/LoginPage';
import { MfaPage } from '../modules/identity/MfaPage';
import { ProfilePage } from '../modules/identity/ProfilePage';
import { UsersPage } from '../modules/admin/UsersPage';
import { UserDetailPage } from '../modules/admin/UserDetailPage';
import { SettingsPage } from '../modules/admin/SettingsPage';
import { FarmDashboardPage } from '../modules/farm/FarmDashboardPage';
import { HomePage } from '../modules/shared/HomePage';
import { ForbiddenPage } from '../modules/shared/ForbiddenPage';
import { AppShell } from './AppShell';

function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.accessToken);
  const location = useLocation();
  return token ? children : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

/** Module gate mirroring the backend ScopeGuard.canRead: SYSTEM_ADMIN and MANAGER always pass. */
function RequireRead({ roles, children }: { roles: readonly string[]; children: ReactNode }) {
  const { canRead } = useAccess();
  return canRead(roles) ? children : <Navigate to="/forbidden" replace />;
}

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/mfa', element: <MfaPage /> },
  {
    element: <ProtectedRoute><AppShell /></ProtectedRoute>,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/profile', element: <ProfilePage /> },
      { path: '/forbidden', element: <ForbiddenPage /> },
      { path: '/admin/users', element: <RequireRead roles={ModuleRoles.admin}><UsersPage /></RequireRead> },
      { path: '/admin/users/:id', element: <RequireRead roles={ModuleRoles.admin}><UserDetailPage /></RequireRead> },
      { path: '/admin/settings', element: <RequireRead roles={ModuleRoles.admin}><SettingsPage /></RequireRead> },
      { path: '/farm', element: <RequireRead roles={ModuleRoles.farm}><FarmDashboardPage /></RequireRead> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
