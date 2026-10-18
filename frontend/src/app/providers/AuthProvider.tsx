import { CircularProgress, Box } from '@mui/material';
import { useEffect, type PropsWithChildren } from 'react';
import { refreshSession } from '../../shared/api/client';
import { getRefreshToken, useAuthStore } from '../../shared/auth/store';

export function AuthProvider({ children }: PropsWithChildren) {
  const { initialized, accessTokenExpiresAt, setInitialized } = useAuthStore();

  useEffect(() => {
    if (!getRefreshToken()) {
      setInitialized(true);
      return;
    }
    void refreshSession().catch(() => undefined);
  }, [setInitialized]);

  useEffect(() => {
    if (!accessTokenExpiresAt) return;
    const delay = Math.max(0, accessTokenExpiresAt - Date.now() - 60_000);
    const timer = window.setTimeout(() => void refreshSession().catch(() => undefined), delay);
    return () => window.clearTimeout(timer);
  }, [accessTokenExpiresAt]);

  if (!initialized) return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><CircularProgress /></Box>;
  return children;
}
