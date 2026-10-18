import { Button, Card, CardContent, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export function SettingsPage() {
  const { t } = useTranslation('admin');
  const { t: tc } = useTranslation();
  return <><Typography variant="h4" gutterBottom>{t('settingsTitle')}</Typography><Card><CardContent><Stack spacing={3} sx={{ maxWidth: 480 }}><Typography variant="h6">{t('general')}</Typography><TextField select label={t('timezone')} defaultValue="Europe/Bucharest"><MenuItem value="Europe/Bucharest">Europe/Bucharest</MenuItem><MenuItem value="UTC">UTC</MenuItem></TextField><TextField select label={t('dateFormat')} defaultValue="DD.MM.YYYY"><MenuItem value="DD.MM.YYYY">DD.MM.YYYY</MenuItem><MenuItem value="YYYY-MM-DD">YYYY-MM-DD</MenuItem></TextField><Button variant="contained" sx={{ alignSelf: 'flex-start' }}>{tc('save')}</Button></Stack></CardContent></Card></>;
}
