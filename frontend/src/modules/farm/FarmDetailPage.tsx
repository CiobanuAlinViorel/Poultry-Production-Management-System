import { Add, ArrowBack } from '@mui/icons-material';
import { Button, Card, MenuItem, Stack, Tab, Table, TableBody, TableCell, TableHead, TableRow, Tabs, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useBreeds, useCloseLot, useCreateHouse, useCreateLot, useCreateSilo, useFarm, useHouses, useLots, useSilos } from '../../shared/api/hooks';
import type { HouseRequest, LotRequest, SiloRequest } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip, formatDate } from '../../shared/ui';

const apiMessage = (e: unknown) => (e as { response?: { data?: { message?: string } } }).response?.data?.message;
type TabKey = 'houses' | 'silos' | 'lots';

export function FarmDetailPage() {
  const { t } = useTranslation('farm');
  const { t: tc } = useTranslation();
  const { farmId = '' } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const tab = (params.get('tab') as TabKey) || 'houses';
  const { canWrite } = useAccess();
  const writable = canWrite(ModuleRoles.farm);
  const farm = useFarm(farmId);
  const houses = useHouses(farmId);
  const silos = useSilos(farmId);
  const lots = useLots(farmId);
  const closeLot = useCloseLot(farmId);
  const [dialog, setDialog] = useState<TabKey | null>(null);

  const f = farm.data;
  const houseName = (id: string) => houses.data?.find((h) => h.id === id)?.name ?? id;

  return (
    <>
      <PageHeader title={f?.name ?? t('farm')} subtitle={f ? `${f.code} · ${[f.address, f.county].filter(Boolean).join(', ') || '—'}` : undefined}
        actions={<>
          <Button startIcon={<ArrowBack />} onClick={() => navigate('/farm')}>{tc('back')}</Button>
          {writable && <Button variant="contained" startIcon={<Add />} onClick={() => setDialog(tab)}>{t(`new.${tab}`)}</Button>}
        </>} />
      <QueryState isLoading={farm.isLoading} isError={farm.isError}>
        <Tabs value={tab} onChange={(_, v: TabKey) => setParams({ tab: v })} sx={{ mb: 2 }}>
          <Tab value="houses" label={`${t('houses')} (${houses.data?.length ?? 0})`} />
          <Tab value="silos" label={`${t('silos')} (${silos.data?.length ?? 0})`} />
          <Tab value="lots" label={`${t('lots')} (${lots.data?.length ?? 0})`} />
        </Tabs>
        <Card sx={{ overflowX: 'auto' }}>
          {tab === 'houses' && (
            <QueryState isLoading={houses.isLoading} isError={houses.isError} isEmpty={houses.data?.length === 0} emptyMessage={t('noHouses')}>
              <Table size="small">
                <TableHead><TableRow><TableCell>{t('code')}</TableCell><TableCell>{t('name')}</TableCell><TableCell align="right">{t('capacity')}</TableCell><TableCell align="right">{t('areaM2')}</TableCell><TableCell>{t('status')}</TableCell></TableRow></TableHead>
                <TableBody>{houses.data?.map((h) => <TableRow key={h.id} hover><TableCell>{h.code}</TableCell><TableCell>{h.name}</TableCell><TableCell align="right">{h.capacity.toLocaleString()}</TableCell><TableCell align="right">{h.areaM2}</TableCell><TableCell><StatusChip value={h.active} label={h.active ? t('active') : t('inactive')} /></TableCell></TableRow>)}</TableBody>
              </Table>
            </QueryState>
          )}
          {tab === 'silos' && (
            <QueryState isLoading={silos.isLoading} isError={silos.isError} isEmpty={silos.data?.length === 0} emptyMessage={t('noSilos')}>
              <Table size="small">
                <TableHead><TableRow><TableCell>{t('code')}</TableCell><TableCell>{t('name')}</TableCell><TableCell align="right">{t('capacityKg')}</TableCell></TableRow></TableHead>
                <TableBody>{silos.data?.map((s) => <TableRow key={s.id} hover><TableCell>{s.code}</TableCell><TableCell>{s.name}</TableCell><TableCell align="right">{s.capacityKg.toLocaleString()}</TableCell></TableRow>)}</TableBody>
              </Table>
            </QueryState>
          )}
          {tab === 'lots' && (
            <QueryState isLoading={lots.isLoading} isError={lots.isError} isEmpty={lots.data?.length === 0} emptyMessage={t('noLots')}>
              <Table size="small">
                <TableHead><TableRow><TableCell>{t('house')}</TableCell><TableCell>{t('breed')}</TableCell><TableCell>{t('arrivalDate')}</TableCell><TableCell align="right">{t('birds')}</TableCell><TableCell>{t('expectedCatch')}</TableCell><TableCell>{t('status')}</TableCell><TableCell /></TableRow></TableHead>
                <TableBody>
                  {lots.data?.map((l) => (
                    <TableRow key={l.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/farm/${farmId}/lots/${l.id}`)}>
                      <TableCell>{houseName(l.houseId)}</TableCell><TableCell>{l.breedCode}</TableCell><TableCell>{formatDate(l.arrivalDate)}</TableCell>
                      <TableCell align="right">{(l.femaleCount + l.maleCount).toLocaleString()}</TableCell><TableCell>{formatDate(l.expectedCatchDate)}</TableCell>
                      <TableCell><StatusChip value={l.status} label={t(`lotStatus.${l.status}`, l.status)} /></TableCell>
                      <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button size="small" onClick={() => navigate(`/farm/${farmId}/lots/${l.id}`)}>{t('open')}</Button>
                          {writable && l.status === 'ACTIVE' && <Button size="small" color="warning" onClick={() => closeLot.mutate(l.id)}>{t('closeLot')}</Button>}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </QueryState>
          )}
        </Card>
      </QueryState>
      {dialog === 'houses' && <HouseDialog farmId={farmId} onClose={() => setDialog(null)} />}
      {dialog === 'silos' && <SiloDialog farmId={farmId} onClose={() => setDialog(null)} />}
      {dialog === 'lots' && <LotDialog farmId={farmId} onClose={() => setDialog(null)} />}
    </>
  );
}

function HouseDialog({ farmId, onClose }: { farmId: string; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const create = useCreateHouse(farmId);
  const [form, setForm] = useState({ code: '', name: '', capacity: '', areaM2: '' });
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    const body: HouseRequest = { code: form.code, name: form.name, capacity: Number(form.capacity), areaM2: Number(form.areaM2), active: true };
    create.mutate(body, { onSuccess: onClose, onError: (e) => setError(apiMessage(e) ?? t('createFailed')) });
  };
  return (
    <FormDialog open title={t('new.houses')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField required label={t('code')} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} autoFocus />
      <TextField required label={t('name')} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      <TextField required type="number" label={t('capacity')} value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} />
      <TextField required type="number" label={t('areaM2')} value={form.areaM2} onChange={(e) => setForm({ ...form, areaM2: e.target.value })} />
    </FormDialog>
  );
}

function SiloDialog({ farmId, onClose }: { farmId: string; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const create = useCreateSilo(farmId);
  const [form, setForm] = useState({ code: '', name: '', capacityKg: '' });
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    const body: SiloRequest = { code: form.code, name: form.name, capacityKg: Number(form.capacityKg) };
    create.mutate(body, { onSuccess: onClose, onError: (e) => setError(apiMessage(e) ?? t('createFailed')) });
  };
  return (
    <FormDialog open title={t('new.silos')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField required label={t('code')} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} autoFocus />
      <TextField required label={t('name')} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      <TextField required type="number" label={t('capacityKg')} value={form.capacityKg} onChange={(e) => setForm({ ...form, capacityKg: e.target.value })} />
    </FormDialog>
  );
}

function LotDialog({ farmId, onClose }: { farmId: string; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const create = useCreateLot(farmId);
  const houses = useHouses(farmId);
  const breeds = useBreeds();
  const today = new Date().toISOString().slice(0, 10);
  const [form, setForm] = useState({ houseId: '', breedCode: '', arrivalDate: today, femaleCount: '0', maleCount: '0', expectedCatchDate: '' });
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    const body: LotRequest = { houseId: form.houseId, breedCode: form.breedCode, arrivalDate: form.arrivalDate, femaleCount: Number(form.femaleCount), maleCount: Number(form.maleCount), expectedCatchDate: form.expectedCatchDate || undefined };
    create.mutate(body, { onSuccess: onClose, onError: (e) => setError(apiMessage(e) ?? t('createFailed')) });
  };
  return (
    <FormDialog open title={t('new.lots')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField select required label={t('house')} value={form.houseId} onChange={(e) => setForm({ ...form, houseId: e.target.value })} helperText={houses.data?.length === 0 ? t('noHouses') : undefined}>
        {houses.data?.filter((h) => h.active).map((h) => <MenuItem key={h.id} value={h.id}>{h.code} — {h.name} ({h.capacity.toLocaleString()})</MenuItem>)}
      </TextField>
      {breeds.data?.length
        ? <TextField select required label={t('breed')} value={form.breedCode} onChange={(e) => setForm({ ...form, breedCode: e.target.value })}>{breeds.data.map((b) => <MenuItem key={b.id} value={b.code}>{b.code} — {b.name}</MenuItem>)}</TextField>
        : <TextField required label={t('breed')} value={form.breedCode} onChange={(e) => setForm({ ...form, breedCode: e.target.value })} helperText={t('breedCodeHelp')} />}
      <TextField required type="date" label={t('arrivalDate')} value={form.arrivalDate} onChange={(e) => setForm({ ...form, arrivalDate: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
      <Stack direction="row" spacing={2}>
        <TextField required type="number" label={t('femaleCount')} value={form.femaleCount} onChange={(e) => setForm({ ...form, femaleCount: e.target.value })} sx={{ flex: 1 }} />
        <TextField required type="number" label={t('maleCount')} value={form.maleCount} onChange={(e) => setForm({ ...form, maleCount: e.target.value })} sx={{ flex: 1 }} />
      </Stack>
      <TextField type="date" label={t('expectedCatch')} value={form.expectedCatchDate} onChange={(e) => setForm({ ...form, expectedCatchDate: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
      <Typography variant="caption" color="text.secondary">{t('lotHelp')}</Typography>
    </FormDialog>
  );
}
