import type { ReactNode } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../shared/auth/store';
import { LoginPage } from '../modules/identity/LoginPage';
import { MfaPage } from '../modules/identity/MfaPage';
import { ProfilePage } from '../modules/identity/ProfilePage';
import { UsersPage } from '../modules/admin/UsersPage';
import { UserDetailPage } from '../modules/admin/UserDetailPage';
import { SettingsPage } from '../modules/admin/SettingsPage';
import { FarmDashboardPage } from '../modules/farm/FarmDashboardPage';
import { HomePage } from '../modules/shared/HomePage';
import { AppShell } from './AppShell';

function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.accessToken);
  const location = useLocation();
  return token ? children : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/mfa', element: <MfaPage /> },
  {
    element: <ProtectedRoute><AppShell /></ProtectedRoute>,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/profile', element: <ProfilePage /> },
      { path: '/admin/users', element: <UsersPage /> },
      { path: '/admin/users/:id', element: <UserDetailPage /> },
      { path: '/admin/settings', element: <SettingsPage /> },
      { path: '/farm', element: <FarmDashboardPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
