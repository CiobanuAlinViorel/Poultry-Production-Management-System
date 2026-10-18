import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Box, Button, Card, CardContent, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { apiClient, type AuthResponse } from '../../shared/api/client';
import { getDeviceId, useAuthStore } from '../../shared/auth/store';

const schema = z.object({ email: z.email(), password: z.string().min(1) });
type LoginValues = z.infer<typeof schema>;

export function LoginPage() {
  const { t } = useTranslation('identity');
  const { t: te } = useTranslation('errors');
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const [error, setError] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({ resolver: zodResolver(schema) });

  const submit = async (values: LoginValues) => {
    setError(false);
    try {
      const { data } = await apiClient.post<AuthResponse>('/api/identity/auth/login', { ...values, deviceId: getDeviceId() });
      if (data.mfaRequired) {
        navigate('/mfa', { state: { mfaToken: data.mfaToken } });
        return;
      }
      setSession(data.accessToken, data.expiresIn, data.refreshToken, data.user);
      const destination = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(destination, { replace: true });
    } catch { setError(true); }
  };

  return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2 }}><Card sx={{ width: '100%', maxWidth: 420 }}><CardContent sx={{ p: 4 }}><Typography variant="h4" gutterBottom>{t('loginTitle')}</Typography><Typography color="text.secondary" sx={{ mb: 3 }}>{t('loginSubtitle')}</Typography>{error && <Alert severity="error" sx={{ mb: 2 }}>{te('invalidCredentials')}</Alert>}<Box component="form" onSubmit={handleSubmit(submit)} noValidate><TextField fullWidth label={t('email')} type="email" autoComplete="email" margin="normal" error={Boolean(errors.email)} helperText={errors.email ? te('emailInvalid') : ''} {...register('email')} /><TextField fullWidth label={t('password')} type="password" autoComplete="current-password" margin="normal" error={Boolean(errors.password)} helperText={errors.password ? te('passwordRequired') : ''} {...register('password')} /><Button fullWidth type="submit" variant="contained" size="large" disabled={isSubmitting} sx={{ mt: 3 }}>{isSubmitting ? t('loggingIn') : t('submit')}</Button></Box></CardContent></Card></Box>;
}
