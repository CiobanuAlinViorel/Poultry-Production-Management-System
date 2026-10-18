import { Add, ArrowBack, Delete } from '@mui/icons-material';
import { Button, Card, CardContent, CardHeader, Grid2 as Grid, IconButton, MenuItem, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Tooltip, Typography } from '@mui/material';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { useAssignRole, useAssignments, useRevokeAssignment, useRoles, useSites, useUser, useUserAction, type UserAction } from '../../shared/api/hooks';
import type { ScopeType } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess, useAuthStore } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip, formatDate } from '../../shared/ui';

export function UserDetailPage() {
  const { t } = useTranslation('admin');
  const { t: tc } = useTranslation();
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const { canWrite } = useAccess();
  const me = useAuthStore((s) => s.user);
  const writable = canWrite(ModuleRoles.admin);
  const user = useUser(id);
  const assignments = useAssignments(id, true);
  const action = useUserAction(id);
  const revoke = useRevokeAssignment();
  const [assignOpen, setAssignOpen] = useState(false);

  const act = (a: UserAction) => action.mutate(a);
  const u = user.data;

  return (
    <>
      <PageHeader title={u?.displayName ?? u?.email ?? t('userDetails')} subtitle={u?.email}
        actions={<Button startIcon={<ArrowBack />} onClick={() => navigate('/admin/users')}>{tc('back')}</Button>} />
      <QueryState isLoading={user.isLoading} isError={user.isError}>
        {u && (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 5 }}>
              <Card>
                <CardHeader title={t('account')} />
                <CardContent>
                  <Stack spacing={1}>
                    <Row label={t('status')}><StatusChip value={u.status} label={t(`userStatus.${u.status}`)} /></Row>
                    <Row label={t('email')}>{u.email}</Row>
                    <Row label={t('username')}>{u.username ?? '—'}</Row>
                    <Row label={t('phone')}>{u.phone ?? '—'}</Row>
                    <Row label={t('locale')}>{u.locale ?? '—'}</Row>
                    <Row label={t('lastLogin')}>{formatDate(u.lastLoginAt, true)}</Row>
                    <Row label={t('createdAt')}>{formatDate(u.createdAt, true)}</Row>
                    <Row label={t('userId')}><Typography variant="caption" sx={{ fontFamily: 'monospace' }}>{u.id}</Typography></Row>
                  </Stack>
                  {writable && (
                    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" sx={{ mt: 3 }}>
                      {u.status === 'DISABLED'
                        ? <Button size="small" variant="outlined" onClick={() => act('enable')}>{t('actions.enable')}</Button>
                        : <Button size="small" variant="outlined" color="warning" disabled={u.id === me?.id} onClick={() => act('disable')}>{t('actions.disable')}</Button>}
                      {u.status === 'LOCKED' && <Button size="small" variant="outlined" onClick={() => act('unlock')}>{t('actions.unlock')}</Button>}
                      <Button size="small" variant="outlined" onClick={() => act('reset-password')}>{t('actions.resetPassword')}</Button>
                      <Button size="small" variant="outlined" onClick={() => act('force-password-change')}>{t('actions.forcePasswordChange')}</Button>
                      <Button size="small" variant="outlined" onClick={() => act('mfa/reset')}>{t('actions.resetMfa')}</Button>
                    </Stack>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, md: 7 }}>
              <Card>
                <CardHeader title={t('roles')} subheader={t('rolesSubtitle')}
                  action={writable && <Button size="small" variant="contained" startIcon={<Add />} onClick={() => setAssignOpen(true)}>{t('assignRole')}</Button>} />
                <CardContent sx={{ pt: 0, overflowX: 'auto' }}>
                  <QueryState isLoading={assignments.isLoading} isError={assignments.isError} isEmpty={assignments.data?.length === 0} emptyMessage={t('noRoles')}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>{t('role')}</TableCell>
                          <TableCell>{t('scope')}</TableCell>
                          <TableCell>{t('assignedAt')}</TableCell>
                          <TableCell>{t('status')}</TableCell>
                          {writable && <TableCell />}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {assignments.data?.map((a) => (
                          <TableRow key={a.id} sx={{ opacity: a.active ? 1 : 0.55 }}>
                            <TableCell><Typography variant="body2" fontWeight={600}>{a.roleCode}</Typography><Typography variant="caption" color="text.secondary">{a.roleName}</Typography></TableCell>
                            <TableCell>{a.scopeType === 'GLOBAL' ? t('scopeGlobal') : `${t(`scopeType.${a.scopeType}`)}: ${a.scopeName ?? a.scopeId}`}</TableCell>
                            <TableCell>{formatDate(a.assignedAt)}</TableCell>
                            <TableCell><StatusChip value={a.active} label={a.active ? t('active') : (a.revokedAt ? t('revoked') : t('inactive'))} /></TableCell>
                            {writable && <TableCell align="right">{a.active && (
                              <Tooltip title={t('revoke')}><IconButton size="small" onClick={() => revoke.mutate(a.id)}><Delete fontSize="small" /></IconButton></Tooltip>
                            )}</TableCell>}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </QueryState>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}
      </QueryState>
      <AssignRoleDialog open={assignOpen} userId={id} onClose={() => setAssignOpen(false)} />
    </>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return <Stack direction="row" justifyContent="space-between" spacing={2}><Typography color="text.secondary">{label}</Typography><Typography component="div" textAlign="right">{children}</Typography></Stack>;
}

function AssignRoleDialog({ open, userId, onClose }: { open: boolean; userId: string; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const roles = useRoles(open);
  const assign = useAssignRole();
  const [roleCode, setRoleCode] = useState('');
  const [scopeType, setScopeType] = useState<ScopeType>('GLOBAL');
  const [scopeId, setScopeId] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const role = useMemo(() => roles.data?.find((r) => r.code === roleCode), [roles.data, roleCode]);
  const allowed = role?.allowedScopeTypes ?? [];
  const sites = useSites(scopeType !== 'GLOBAL' ? scopeType : undefined);

  const pickRole = (code: string) => {
    setRoleCode(code);
    const r = roles.data?.find((x) => x.code === code);
    const first = r?.allowedScopeTypes[0] ?? 'GLOBAL';
    setScopeType(first);
    setScopeId('');
  };
  const submit = () => {
    setError(null);
    assign.mutate({ userId, roleCode, scopeType, scopeId: scopeType === 'GLOBAL' ? undefined : scopeId, reason: reason || undefined }, {
      onSuccess: () => { onClose(); setRoleCode(''); setReason(''); },
      onError: (e: unknown) => setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? t('createFailed')),
    });
  };
  return (
    <FormDialog open={open} title={t('assignRole')} onClose={onClose} onSubmit={submit} submitting={assign.isPending} error={error} submitLabel={t('assign')}>
      <TextField select required label={t('role')} value={roleCode} onChange={(e) => pickRole(e.target.value)}>
        {roles.data?.filter((r) => r.active).map((r) => <MenuItem key={r.code} value={r.code}>{r.code} — {r.nameRo}</MenuItem>)}
      </TextField>
      <TextField select required label={t('scope')} value={scopeType} onChange={(e) => { setScopeType(e.target.value as ScopeType); setScopeId(''); }} disabled={!role}>
        {allowed.map((s) => <MenuItem key={s} value={s}>{s === 'GLOBAL' ? t('scopeGlobal') : t(`scopeType.${s}`)}</MenuItem>)}
      </TextField>
      {scopeType !== 'GLOBAL' && (
        <TextField select required label={t('site')} value={scopeId} onChange={(e) => setScopeId(e.target.value)}
          helperText={sites.data?.length === 0 ? t('noSitesForScope') : undefined}>
          {sites.data?.map((s) => <MenuItem key={s.siteId} value={s.siteId}>{s.code} — {s.name ?? s.siteId}</MenuItem>)}
        </TextField>
      )}
      <TextField label={t('reason')} value={reason} onChange={(e) => setReason(e.target.value)} multiline minRows={2} />
    </FormDialog>
  );
}
