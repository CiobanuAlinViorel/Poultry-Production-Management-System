import { Box, Button, Card, CardContent, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { apiClient, type AuthResponse } from '../../shared/api/client';
import { getDeviceId, useAuthStore } from '../../shared/auth/store';

export function MfaPage() {
  const { t } = useTranslation('identity');
  const navigate = useNavigate();
  const { state } = useLocation();
  const mfaToken = (state as { mfaToken?: string } | null)?.mfaToken;
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const setSession = useAuthStore((value) => value.setSession);
  if (!mfaToken) return <Navigate to="/login" replace />;
  const verify = async () => {
    setBusy(true);
    try {
      const { data } = await apiClient.post<AuthResponse>('/api/identity/auth/mfa', { mfaToken, code, deviceId: getDeviceId() });
      setSession(data.accessToken, data.expiresIn, data.refreshToken, data.user);
      navigate('/', { replace: true });
    } finally { setBusy(false); }
  };
  return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2 }}><Card sx={{ width: '100%', maxWidth: 420 }}><CardContent sx={{ p: 4 }}><Typography variant="h5" gutterBottom>{t('mfaTitle')}</Typography><TextField fullWidth label={t('mfaCode')} value={code} onChange={(event) => setCode(event.target.value)} inputProps={{ inputMode: 'numeric', maxLength: 8 }} sx={{ my: 2 }} /><Button fullWidth variant="contained" disabled={busy || !code} onClick={() => void verify()}>{busy ? t('verifying') : t('verify')}</Button></CardContent></Card></Box>;
}
