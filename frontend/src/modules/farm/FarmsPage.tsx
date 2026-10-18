import { Add, GridView, TableRows } from '@mui/icons-material';
import { Button, Card, CardActionArea, CardContent, Grid2 as Grid, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useCreateFarm, useFarms } from '../../shared/api/hooks';
import type { FarmRequest } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip } from '../../shared/ui';

export function FarmsPage() {
  const { t } = useTranslation('farm');
  const { t: tc } = useTranslation();
  const navigate = useNavigate();
  const { canWrite } = useAccess();
  const writable = canWrite(ModuleRoles.farm);
  const farms = useFarms();
  const [view, setView] = useState<'cards' | 'table'>((localStorage.getItem('ppms.farms.view') as 'cards' | 'table') ?? 'cards');
  const [createOpen, setCreateOpen] = useState(false);
  const [q, setQ] = useState('');
  const list = (farms.data ?? []).filter((f) => !q || `${f.code} ${f.name} ${f.county ?? ''}`.toLowerCase().includes(q.toLowerCase()));
  const changeView = (v: 'cards' | 'table' | null) => { if (v) { setView(v); localStorage.setItem('ppms.farms.view', v); } };
  const createButton = writable && <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>{t('newFarm')}</Button>;

  return (
    <>
      <PageHeader title={t('farms')} subtitle={t('farmsSubtitle')} actions={<>
        <ToggleButtonGroup size="small" exclusive value={view} onChange={(_, v) => changeView(v)}>
          <ToggleButton value="cards" aria-label={t('viewCards')}><GridView fontSize="small" /></ToggleButton>
          <ToggleButton value="table" aria-label={t('viewTable')}><TableRows fontSize="small" /></ToggleButton>
        </ToggleButtonGroup>
        {createButton}
      </>} />
      <TextField size="small" label={tc('search')} value={q} onChange={(e) => setQ(e.target.value)} sx={{ mb: 2, minWidth: 260 }} />
      <QueryState isLoading={farms.isLoading} isError={farms.isError} isEmpty={list.length === 0} emptyMessage={t('noFarms')} emptyAction={createButton}>
        {view === 'cards' ? (
          <Grid container spacing={2}>
            {list.map((f) => (
              <Grid key={f.id} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
                <Card sx={{ height: '100%' }}>
                  <CardActionArea onClick={() => navigate(`/farm/${f.id}`)} sx={{ height: '100%' }}>
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
                        <Typography variant="h6" noWrap>{f.name}</Typography>
                        <StatusChip value={f.active} label={f.active ? t('active') : t('inactive')} />
                      </Stack>
                      <Typography variant="body2" color="text.secondary">{f.code}</Typography>
                      <Typography variant="body2" sx={{ mt: 1 }}>{[f.address, f.county].filter(Boolean).join(', ') || '—'}</Typography>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
        ) : (
          <Card sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead><TableRow><TableCell>{t('code')}</TableCell><TableCell>{t('name')}</TableCell><TableCell>{t('address')}</TableCell><TableCell>{t('county')}</TableCell><TableCell>{t('status')}</TableCell></TableRow></TableHead>
              <TableBody>
                {list.map((f) => (
                  <TableRow key={f.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/farm/${f.id}`)}>
                    <TableCell>{f.code}</TableCell><TableCell>{f.name}</TableCell><TableCell>{f.address ?? '—'}</TableCell><TableCell>{f.county ?? '—'}</TableCell>
                    <TableCell><StatusChip value={f.active} label={f.active ? t('active') : t('inactive')} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
        )}
      </QueryState>
      <FarmDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  );
}

function FarmDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const create = useCreateFarm();
  const [form, setForm] = useState<FarmRequest>({ code: '', name: '', address: '', county: '', active: true });
  const [error, setError] = useState<string | null>(null);
  const set = (k: keyof FarmRequest) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });
  const submit = () => {
    setError(null);
    create.mutate({ ...form, address: form.address || undefined, county: form.county || undefined }, {
      onSuccess: () => { onClose(); setForm({ code: '', name: '', address: '', county: '', active: true }); },
      onError: (e: unknown) => setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? t('createFailed')),
    });
  };
  return (
    <FormDialog open={open} title={t('newFarm')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField required label={t('code')} value={form.code} onChange={set('code')} autoFocus />
      <TextField required label={t('name')} value={form.name} onChange={set('name')} />
      <TextField label={t('address')} value={form.address} onChange={set('address')} />
      <TextField label={t('county')} value={form.county} onChange={set('county')} />
    </FormDialog>
  );
}
