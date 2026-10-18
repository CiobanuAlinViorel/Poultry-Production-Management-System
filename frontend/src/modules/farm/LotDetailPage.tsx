import { Add, ArrowBack } from '@mui/icons-material';
import { Button, Card, CardContent, CardHeader, Grid2 as Grid, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useDailySheets, useDeliveries, useFarm, useHouses, useLots, useRecordDailySheet, useRecordDelivery } from '../../shared/api/hooks';
import type { DailySheetRequest, FarmDeliveryRequest } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip, formatDate } from '../../shared/ui';

const apiMessage = (e: unknown) => (e as { response?: { data?: { message?: string } } }).response?.data?.message;
const num = (v: string) => (v === '' ? undefined : Number(v));
const today = () => new Date().toISOString().slice(0, 10);

export function LotDetailPage() {
  const { t } = useTranslation('farm');
  const { t: tc } = useTranslation();
  const { farmId = '', lotId = '' } = useParams();
  const navigate = useNavigate();
  const { canWrite } = useAccess();
  const writable = canWrite(ModuleRoles.farm);
  const farm = useFarm(farmId);
  const lots = useLots(farmId);
  const houses = useHouses(farmId);
  const sheets = useDailySheets(lotId);
  const deliveries = useDeliveries(lotId);
  const [dialog, setDialog] = useState<'sheet' | 'delivery' | null>(null);

  const lot = lots.data?.find((l) => l.id === lotId);
  const house = houses.data?.find((h) => h.id === lot?.houseId);
  const placed = lot ? lot.femaleCount + lot.maleCount : 0;
  const mortality = sheets.data?.reduce((s, x) => s + x.mortality, 0) ?? 0;
  const delivered = deliveries.data?.reduce((s, x) => s + x.quantity, 0) ?? 0;
  const lastWeight = sheets.data?.length ? [...sheets.data].sort((a, b) => a.date.localeCompare(b.date)).at(-1)?.avgWeightG : undefined;
  const active = lot?.status === 'ACTIVE';

  return (
    <>
      <PageHeader title={lot ? `${t('lot')} · ${house?.name ?? lot.houseId}` : t('lot')} subtitle={farm.data ? `${farm.data.name} · ${lot?.breedCode ?? ''}` : undefined}
        actions={<>
          <Button startIcon={<ArrowBack />} onClick={() => navigate(`/farm/${farmId}?tab=lots`)}>{tc('back')}</Button>
          {writable && active && <Button variant="outlined" startIcon={<Add />} onClick={() => setDialog('delivery')}>{t('new.delivery')}</Button>}
          {writable && active && <Button variant="contained" startIcon={<Add />} onClick={() => setDialog('sheet')}>{t('new.sheet')}</Button>}
        </>} />
      <QueryState isLoading={lots.isLoading} isError={lots.isError} isEmpty={!lots.isLoading && !lot} emptyMessage={t('noLots')}>
        <Grid container spacing={2}>
          {[
            [t('status'), lot && <StatusChip value={lot.status} label={t(`lotStatus.${lot.status}`, lot.status)} />],
            [t('arrivalDate'), formatDate(lot?.arrivalDate)],
            [t('birdsPlaced'), placed.toLocaleString()],
            [t('mortalityTotal'), `${mortality.toLocaleString()} (${placed ? ((mortality / placed) * 100).toFixed(2) : '0.00'}%)`],
            [t('delivered'), delivered.toLocaleString()],
            [t('lastAvgWeight'), lastWeight ? `${lastWeight} g` : '—'],
          ].map(([label, value], i) => (
            <Grid key={i} size={{ xs: 6, sm: 4, md: 2 }}>
              <Card><CardContent><Typography variant="caption" color="text.secondary">{label}</Typography><Typography variant="h6" component="div">{value}</Typography></CardContent></Card>
            </Grid>
          ))}
          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader title={t('dailySheets')} />
              <CardContent sx={{ pt: 0, overflowX: 'auto' }}>
                <QueryState isLoading={sheets.isLoading} isError={sheets.isError} isEmpty={sheets.data?.length === 0} emptyMessage={t('noSheets')}>
                  <Table size="small">
                    <TableHead><TableRow><TableCell>{t('date')}</TableCell><TableCell align="right">{t('mortality')}</TableCell><TableCell align="right">{t('feedKg')}</TableCell><TableCell align="right">{t('waterLiters')}</TableCell><TableCell align="right">{t('avgWeightG')}</TableCell><TableCell align="right">{t('temperature')}</TableCell><TableCell align="right">{t('humidityPct')}</TableCell><TableCell>{t('comments')}</TableCell></TableRow></TableHead>
                    <TableBody>
                      {[...(sheets.data ?? [])].sort((a, b) => b.date.localeCompare(a.date)).map((s) => (
                        <TableRow key={s.id} hover>
                          <TableCell>{formatDate(s.date)}</TableCell><TableCell align="right">{s.mortality}</TableCell><TableCell align="right">{s.feedKg ?? '—'}</TableCell><TableCell align="right">{s.waterLiters ?? '—'}</TableCell>
                          <TableCell align="right">{s.avgWeightG ?? '—'}</TableCell><TableCell align="right">{s.temperatureMin != null || s.temperatureMax != null ? `${s.temperatureMin ?? '—'} / ${s.temperatureMax ?? '—'}` : '—'}</TableCell>
                          <TableCell align="right">{s.humidityPct ?? '—'}</TableCell><TableCell>{s.comments ?? ''}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </QueryState>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, lg: 4 }}>
            <Card>
              <CardHeader title={t('deliveries')} />
              <CardContent sx={{ pt: 0, overflowX: 'auto' }}>
                <QueryState isLoading={deliveries.isLoading} isError={deliveries.isError} isEmpty={deliveries.data?.length === 0} emptyMessage={t('noDeliveries')}>
                  <Table size="small">
                    <TableHead><TableRow><TableCell>{t('date')}</TableCell><TableCell align="right">{t('quantity')}</TableCell><TableCell align="right">{t('avgWeightKg')}</TableCell><TableCell>{t('fciRef')}</TableCell></TableRow></TableHead>
                    <TableBody>{deliveries.data?.map((d) => <TableRow key={d.id} hover><TableCell>{formatDate(d.dispatchDate)}</TableCell><TableCell align="right">{d.quantity.toLocaleString()}</TableCell><TableCell align="right">{d.avgWeightKg}</TableCell><TableCell>{d.fciRef ?? '—'}</TableCell></TableRow>)}</TableBody>
                  </Table>
                </QueryState>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </QueryState>
      {dialog === 'sheet' && <SheetDialog lotId={lotId} onClose={() => setDialog(null)} />}
      {dialog === 'delivery' && <DeliveryDialog lotId={lotId} onClose={() => setDialog(null)} />}
    </>
  );
}

function SheetDialog({ lotId, onClose }: { lotId: string; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const record = useRecordDailySheet(lotId);
  const [f, setF] = useState({ date: today(), mortality: '0', feedKg: '', waterLiters: '', avgWeightG: '', temperatureMin: '', temperatureMax: '', humidityPct: '', comments: '' });
  const [error, setError] = useState<string | null>(null);
  const field = (k: keyof typeof f, label: string, type = 'number') => <TextField type={type} label={label} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} sx={{ flex: 1 }} slotProps={type === 'date' ? { inputLabel: { shrink: true } } : undefined} />;
  const submit = () => {
    setError(null);
    const body: DailySheetRequest = { date: f.date, mortality: Number(f.mortality), feedKg: num(f.feedKg), waterLiters: num(f.waterLiters), avgWeightG: num(f.avgWeightG), temperatureMin: num(f.temperatureMin), temperatureMax: num(f.temperatureMax), humidityPct: num(f.humidityPct), comments: f.comments || undefined };
    record.mutate(body, { onSuccess: onClose, onError: (e) => setError(apiMessage(e) ?? t('createFailed')) });
  };
  return (
    <FormDialog open title={t('new.sheet')} onClose={onClose} onSubmit={submit} submitting={record.isPending} error={error}>
      <Stack direction="row" spacing={2}>{field('date', t('date'), 'date')}{field('mortality', t('mortality'))}</Stack>
      <Stack direction="row" spacing={2}>{field('feedKg', t('feedKg'))}{field('waterLiters', t('waterLiters'))}{field('avgWeightG', t('avgWeightG'))}</Stack>
      <Stack direction="row" spacing={2}>{field('temperatureMin', t('temperatureMin'))}{field('temperatureMax', t('temperatureMax'))}{field('humidityPct', t('humidityPct'))}</Stack>
      <TextField label={t('comments')} value={f.comments} onChange={(e) => setF({ ...f, comments: e.target.value })} multiline minRows={2} />
    </FormDialog>
  );
}

function DeliveryDialog({ lotId, onClose }: { lotId: string; onClose: () => void }) {
  const { t } = useTranslation('farm');
  const record = useRecordDelivery(lotId);
  const [f, setF] = useState({ dispatchDate: today(), quantity: '', avgWeightKg: '', fciRef: '' });
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    const body: FarmDeliveryRequest = { dispatchDate: f.dispatchDate, quantity: Number(f.quantity), avgWeightKg: Number(f.avgWeightKg), fciRef: f.fciRef || undefined };
    record.mutate(body, { onSuccess: onClose, onError: (e) => setError(apiMessage(e) ?? t('createFailed')) });
  };
  return (
    <FormDialog open title={t('new.delivery')} onClose={onClose} onSubmit={submit} submitting={record.isPending} error={error}>
      <TextField required type="date" label={t('date')} value={f.dispatchDate} onChange={(e) => setF({ ...f, dispatchDate: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
      <TextField required type="number" label={t('quantity')} value={f.quantity} onChange={(e) => setF({ ...f, quantity: e.target.value })} />
      <TextField required type="number" label={t('avgWeightKg')} value={f.avgWeightKg} onChange={(e) => setF({ ...f, avgWeightKg: e.target.value })} />
      <TextField label={t('fciRef')} value={f.fciRef} onChange={(e) => setF({ ...f, fciRef: e.target.value })} />
    </FormDialog>
  );
}
