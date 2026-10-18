import { AdminPanelSettings, Agriculture, ArrowForward, Egg, Factory, Security, Warehouse } from '@mui/icons-material';
import { Button, Card, CardActionArea, CardContent, Grid2 as Grid, Skeleton, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useFarms, useModuleSites, useRoles, useUsers } from '../../shared/api/hooks';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess, useAuthStore } from '../../shared/auth/store';
import { PageHeader } from '../../shared/ui';

interface Tile { key: string; path: string; icon: ReactNode; roles: readonly string[]; count?: number; loading: boolean; canCreate: boolean }

export function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const { canRead, canWrite } = useAccess();

  const farms = useFarms(canRead(ModuleRoles.farm));
  const parentFlock = useModuleSites('parent-flock', canRead(ModuleRoles.parentFlock));
  const hatchery = useModuleSites('hatchery', canRead(ModuleRoles.hatchery));
  const slaughterhouse = useModuleSites('slaughterhouse', canRead(ModuleRoles.slaughterhouse));
  const adminVisible = canRead(ModuleRoles.admin);
  const users = useUsers({ page: 0, size: 1 }, adminVisible);
  const roles = useRoles(adminVisible);

  const tiles: Tile[] = [
    { key: 'parentFlock', path: '/parent-flock', icon: <Egg color="primary" />, roles: ModuleRoles.parentFlock, count: parentFlock.data?.length, loading: parentFlock.isLoading, canCreate: canWrite(ModuleRoles.parentFlock) },
    { key: 'hatchery', path: '/hatchery', icon: <Warehouse color="primary" />, roles: ModuleRoles.hatchery, count: hatchery.data?.length, loading: hatchery.isLoading, canCreate: canWrite(ModuleRoles.hatchery) },
    { key: 'farm', path: '/farm', icon: <Agriculture color="primary" />, roles: ModuleRoles.farm, count: farms.data?.length, loading: farms.isLoading, canCreate: canWrite(ModuleRoles.farm) },
    { key: 'slaughterhouse', path: '/slaughterhouse', icon: <Factory color="primary" />, roles: ModuleRoles.slaughterhouse, count: slaughterhouse.data?.length, loading: slaughterhouse.isLoading, canCreate: canWrite(ModuleRoles.slaughterhouse) },
    { key: 'users', path: '/admin/users', icon: <AdminPanelSettings color="primary" />, roles: ModuleRoles.admin, count: users.data?.totalElements, loading: users.isLoading, canCreate: canWrite(ModuleRoles.admin) },
    { key: 'roles', path: '/admin/roles', icon: <Security color="primary" />, roles: ModuleRoles.admin, count: roles.data?.length, loading: roles.isLoading, canCreate: canWrite(ModuleRoles.admin) },
  ].filter((tile) => canRead(tile.roles));

  return (
    <>
      <PageHeader title={t('dashboard')} subtitle={t('home.welcome', { name: user?.displayName ?? user?.email ?? '' })} />
      {tiles.length === 0 && <Typography color="text.secondary">{t('home.noModules')}</Typography>}
      <Grid container spacing={2}>
        {tiles.map((tile) => (
          <Grid key={tile.key} size={{ xs: 12, sm: 6, lg: 4 }}>
            <Card sx={{ height: '100%' }}>
              <CardActionArea onClick={() => navigate(tile.path)} sx={{ height: '100%', alignItems: 'stretch' }}>
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>{tile.icon}<Typography variant="h6">{t(`home.tiles.${tile.key}.title`)}</Typography></Stack>
                  <Typography variant="body2" color="text.secondary" sx={{ minHeight: 40 }}>{t(`home.tiles.${tile.key}.description`)}</Typography>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-end" sx={{ mt: 2 }}>
                    {tile.loading ? <Skeleton width={60} height={40} /> : <Typography variant="h4">{tile.count ?? '—'}</Typography>}
                    <Button size="small" endIcon={<ArrowForward />} component="span">{tile.canCreate ? t('home.manage') : t('home.view')}</Button>
                  </Stack>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
