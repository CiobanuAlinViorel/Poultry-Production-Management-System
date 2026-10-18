import { Button, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';

export function NotFoundPage() {
  const { t } = useTranslation();
  const { t: te } = useTranslation('errors');
  const navigate = useNavigate();
  const { pathname } = useLocation();
  return (
    <Stack spacing={2} sx={{ maxWidth: 480 }}>
      <Typography variant="h4">{te('notFound')}</Typography>
      <Typography color="text.secondary" sx={{ fontFamily: 'monospace' }}>{pathname}</Typography>
      <Button variant="contained" onClick={() => navigate('/')} sx={{ alignSelf: 'flex-start' }}>{t('home')}</Button>
    </Stack>
  );
}
