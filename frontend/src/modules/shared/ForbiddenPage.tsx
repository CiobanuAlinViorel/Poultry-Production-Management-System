import { Alert, Button, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

export function ForbiddenPage() {
  const { t } = useTranslation();
  const { t: te } = useTranslation('errors');
  const navigate = useNavigate();
  return <Stack spacing={2} sx={{ maxWidth: 480 }}><Typography variant="h4">{te('forbidden')}</Typography><Alert severity="warning">{te('forbiddenDetail')}</Alert><Button variant="contained" onClick={() => navigate('/')}>{t('home')}</Button></Stack>;
}
