import { Button, Card, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../shared/api/client';
import { useAuthStore } from '../../shared/auth/store';

export function ProfilePage() {
  const { t } = useTranslation('identity');
  const { t: tc } = useTranslation();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const logoutAll = async () => {
    try { await apiClient.post('/api/identity/auth/logout-all'); } finally { clearSession(); navigate('/login', { replace: true }); }
  };
  return <><Typography variant="h4" gutterBottom>{t('profileTitle')}</Typography><Card><CardContent><Typography variant="h6">{user?.displayName ?? user?.email}</Typography><Typography color="text.secondary">{user?.email}</Typography><Typography sx={{ mt: 3, mb: 1 }}>{t('roles')}</Typography><Stack direction="row" spacing={1}>{user?.roles.map((role) => <Chip key={role} label={role} />)}</Stack><Button color="error" variant="outlined" sx={{ mt: 4 }} onClick={() => void logoutAll()}>{tc('logoutAll')}</Button></CardContent></Card></>;
}
