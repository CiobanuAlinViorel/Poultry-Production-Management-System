import { Add } from '@mui/icons-material';
import { Button, Card, CardContent, Grid2 as Grid, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCreateModuleSite, useModuleSites, type SiteModule } from '../../shared/api/hooks';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip } from '../../shared/ui';

/** Extra numeric field each module's create-site request requires besides code/name/address/county. */
const MODULE_CONFIG: Record<SiteModule, { roles: readonly string[]; extraField?: { key: string; labelKey: string; required: boolean } }> = {
  'parent-flock': { roles: ModuleRoles.parentFlock, extraField: { key: 'capacityHouses', labelKey: 'capacityHouses', required: true } },
  hatchery: { roles: ModuleRoles.hatchery, extraField: { key: 'weeklySetCapacity', labelKey: 'weeklySetCapacity', required: false } },
  slaughterhouse: { roles: ModuleRoles.slaughterhouse, extraField: { key: 'dailyCapacityBirds', labelKey: 'dailyCapacityBirds', required: false } },
};

export function ModuleSitesPage({ module }: { module: SiteModule }) {
  const { t } = useTranslation('sites');
  const { t: tc } = useTranslation();
  const { canWrite } = useAccess();
  const cfg = MODULE_CONFIG[module];
  const writable = canWrite(cfg.roles);
  const sites = useModuleSites(module);
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const list = (sites.data ?? []).filter((s) => !q || `${s.code} ${s.name ?? ''} ${s.county ?? ''}`.toLowerCase().includes(q.toLowerCase()));
  const createButton = writable && <Button variant="contained" startIcon={<Add />} onClick={() => setOpen(true)}>{t(`${module}.new`)}</Button>;

  return (
    <>
      <PageHeader title={t(`${module}.title`)} subtitle={t(`${module}.subtitle`)} actions={createButton} />
      <TextField size="small" label={tc('search')} value={q} onChange={(e) => setQ(e.target.value)} sx={{ mb: 2, minWidth: 260 }} />
      <QueryState isLoading={sites.isLoading} isError={sites.isError} isEmpty={list.length === 0} emptyMessage={t('noSites')} emptyAction={createButton}>
        <Grid container spacing={2}>
          {list.map((s) => (
            <Grid key={s.id} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
                    <Typography variant="h6" noWrap>{s.name ?? s.code}</Typography>
                    {s.active !== undefined && <StatusChip value={s.active} label={s.active ? t('active') : t('inactive')} />}
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{s.code}</Typography>
                  <Typography variant="body2" sx={{ mt: 1 }}>{[s.address, s.county].filter(Boolean).join(', ') || '—'}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </QueryState>
      {open && <SiteDialog module={module} onClose={() => setOpen(false)} />}
    </>
  );
}

function SiteDialog({ module, onClose }: { module: SiteModule; onClose: () => void }) {
  const { t } = useTranslation('sites');
  const cfg = MODULE_CONFIG[module];
  const create = useCreateModuleSite(module);
  const [f, setF] = useState<Record<string, string>>({ code: '', name: '', address: '', county: '', extra: '' });
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    const body: Record<string, unknown> = { code: f.code, name: f.name, address: f.address || undefined, county: f.county || undefined };
    if (cfg.extraField && f.extra !== '') body[cfg.extraField.key] = Number(f.extra);
    create.mutate(body, { onSuccess: onClose, onError: (e) => setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? t('createFailed')) });
  };
  const field = (k: string, label: string, required = false, type = 'text') => <TextField required={required} type={type} label={label} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} />;
  return (
    <FormDialog open title={t(`${module}.new`)} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      {field('code', t('code'), true)}
      {field('name', t('name'), true)}
      {field('address', t('address'))}
      {field('county', t('county'))}
      {cfg.extraField && field('extra', t(cfg.extraField.labelKey), cfg.extraField.required, 'number')}
    </FormDialog>
  );
}
