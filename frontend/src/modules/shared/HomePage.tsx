import { Card, CardContent, Grid2 as Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export function HomePage() {
  const { t } = useTranslation();
  return <><Typography variant="h4" gutterBottom>{t('dashboard')}</Typography><Grid container spacing={2}>{['Parent flock', 'Hatchery', 'Farms', 'Slaughterhouse'].map((title) => <Grid key={title} size={{ xs: 12, sm: 6, lg: 3 }}><Card><CardContent><Typography variant="h6">{title}</Typography><Typography color="text.secondary">—</Typography></CardContent></Card></Grid>)}</Grid></>;
}
