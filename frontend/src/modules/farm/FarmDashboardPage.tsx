import { useTranslation } from 'react-i18next';
import { Container, Typography, Paper, Box, Button } from '@mui/material';
import { Link } from 'react-router-dom';

export function FarmDashboardPage() {
  const { t } = useTranslation('common');
  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        {t('farm.dashboard.title', 'Ferme')}
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography variant="body1" paragraph>
          {t('farm.dashboard.description', 'Gestionează ferme, hale, loturi și fișe zilnice.')}
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Button variant="contained" component={Link} to="/farm/sites">
            {t('farm.dashboard.sites', 'Ferme')}
          </Button>
          <Button variant="outlined" component={Link} to="/farm/houses">
            {t('farm.dashboard.houses', 'Hale')}
          </Button>
          <Button variant="outlined" component={Link} to="/farm/lots">
            {t('farm.dashboard.lots', 'Loturi')}
          </Button>
        </Box>
      </Paper>
    </Container>
  );
}
