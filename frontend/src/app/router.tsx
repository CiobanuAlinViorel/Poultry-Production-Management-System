import type { ReactNode } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import { useAccess, useAuthStore } from '../shared/auth/store';
import { ModuleRoles } from '../shared/auth/access';
import { LoginPage } from '../modules/identity/LoginPage';
import { MfaPage } from '../modules/identity/MfaPage';
import { ProfilePage } from '../modules/identity/ProfilePage';
import { UsersPage } from '../modules/admin/UsersPage';
import { UserDetailPage } from '../modules/admin/UserDetailPage';
import { RolesPage } from '../modules/admin/RolesPage';
import { SettingsPage } from '../modules/admin/SettingsPage';
import { FarmsPage } from '../modules/farm/FarmsPage';
import { FarmDetailPage } from '../modules/farm/FarmDetailPage';
import { LotDetailPage } from '../modules/farm/LotDetailPage';
import { ModuleSitesPage } from '../modules/sites/ModuleSitesPage';
import { HomePage } from '../modules/shared/HomePage';
import { ForbiddenPage } from '../modules/shared/ForbiddenPage';
import { NotFoundPage } from '../modules/shared/NotFoundPage';
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

const guarded = (roles: readonly string[], element: ReactNode) => <RequireRead roles={roles}>{element}</RequireRead>;

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/mfa', element: <MfaPage /> },
  {
    element: <ProtectedRoute><AppShell /></ProtectedRoute>,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/profile', element: <ProfilePage /> },
      { path: '/forbidden', element: <ForbiddenPage /> },
      { path: '/admin/users', element: guarded(ModuleRoles.admin, <UsersPage />) },
      { path: '/admin/users/:id', element: guarded(ModuleRoles.admin, <UserDetailPage />) },
      { path: '/admin/roles', element: guarded(ModuleRoles.admin, <RolesPage />) },
      { path: '/admin/settings', element: guarded(ModuleRoles.admin, <SettingsPage />) },
      { path: '/farm', element: guarded(ModuleRoles.farm, <FarmsPage />) },
      { path: '/farm/:farmId', element: guarded(ModuleRoles.farm, <FarmDetailPage />) },
      { path: '/farm/:farmId/lots/:lotId', element: guarded(ModuleRoles.farm, <LotDetailPage />) },
      { path: '/parent-flock', element: guarded(ModuleRoles.parentFlock, <ModuleSitesPage module="parent-flock" />) },
      { path: '/hatchery', element: guarded(ModuleRoles.hatchery, <ModuleSitesPage module="hatchery" />) },
      { path: '/slaughterhouse', element: guarded(ModuleRoles.slaughterhouse, <ModuleSitesPage module="slaughterhouse" />) },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
