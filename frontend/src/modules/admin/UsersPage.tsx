import { Add } from '@mui/icons-material';
import { Button, Card, Chip, FormControlLabel, MenuItem, Stack, Switch, Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField } from '@mui/material';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { useAssignmentsByUsers, useCreateUser, useUsers } from '../../shared/api/hooks';
import type { CreateUserRequest, UserStatus } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip, formatDate } from '../../shared/ui';

const STATUSES: UserStatus[] = ['ACTIVE', 'PENDING_ACTIVATION', 'LOCKED', 'DISABLED'];

export function UsersPage() {
  const { t } = useTranslation('admin');
  const { t: tc } = useTranslation();
  const navigate = useNavigate();
  const { canWrite } = useAccess();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<UserStatus | ''>('');
  const [createOpen, setCreateOpen] = useState(false);

  const users = useUsers({ page, size, q, status });
  const ids = useMemo(() => users.data?.content.map((u) => u.id) ?? [], [users.data]);
  const assignments = useAssignmentsByUsers(ids);

  return (
    <>
      <PageHeader title={t('usersTitle')} subtitle={t('usersSubtitle')}
        actions={canWrite(ModuleRoles.admin) && <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>{t('addUser')}</Button>} />
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <TextField size="small" label={tc('search')} value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} sx={{ minWidth: 260 }} />
        <TextField size="small" select label={t('status')} value={status} onChange={(e) => { setStatus(e.target.value as UserStatus | ''); setPage(0); }} sx={{ minWidth: 200 }}>
          <MenuItem value="">{t('allStatuses')}</MenuItem>
          {STATUSES.map((s) => <MenuItem key={s} value={s}>{t(`userStatus.${s}`)}</MenuItem>)}
        </TextField>
      </Stack>
      <Card sx={{ overflowX: 'auto' }}>
        <QueryState isLoading={users.isLoading} isError={users.isError} isEmpty={users.data?.content.length === 0} emptyMessage={t('noUsers')}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('name')}</TableCell>
                <TableCell>{t('email')}</TableCell>
                <TableCell>{t('username')}</TableCell>
                <TableCell>{t('status')}</TableCell>
                <TableCell>{t('roles')}</TableCell>
                <TableCell>{t('lastLogin')}</TableCell>
                <TableCell>{t('createdAt')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.data?.content.map((user) => (
                <TableRow key={user.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/admin/users/${user.id}`)}>
                  <TableCell><Link to={`/admin/users/${user.id}`} onClick={(e) => e.stopPropagation()}>{user.displayName ?? user.email}</Link></TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>{user.username ?? '—'}</TableCell>
                  <TableCell><StatusChip value={user.status} label={t(`userStatus.${user.status}`)} /></TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">
                      {(assignments.data?.[user.id] ?? []).map((a) => (
                        <Chip key={a.id} size="small" variant="outlined" label={a.scopeType === 'GLOBAL' ? a.roleCode : `${a.roleCode} @ ${a.scopeName ?? a.scopeType}`} />
                      ))}
                      {assignments.isSuccess && !(assignments.data?.[user.id]?.length) && <Chip size="small" color="warning" label={t('noRoles')} />}
                    </Stack>
                  </TableCell>
                  <TableCell>{formatDate(user.lastLoginAt, true)}</TableCell>
                  <TableCell>{formatDate(user.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination component="div" count={users.data?.totalElements ?? 0} page={page} rowsPerPage={size}
            onPageChange={(_, p) => setPage(p)} onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }} rowsPerPageOptions={[10, 20, 50]} />
        </QueryState>
      </Card>
      <CreateUserDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  );
}

function CreateUserDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const create = useCreateUser();
  const [form, setForm] = useState<CreateUserRequest>({ email: '', username: '', displayName: '', locale: 'ro', sendActivationEmail: false, initialPassword: '' });
  const [error, setError] = useState<string | null>(null);
  const set = (k: keyof CreateUserRequest) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const submit = () => {
    setError(null);
    const body: CreateUserRequest = { ...form, username: form.username || undefined, displayName: form.displayName || undefined, initialPassword: form.initialPassword || undefined };
    create.mutate(body, {
      onSuccess: () => { onClose(); setForm({ email: '', username: '', displayName: '', locale: 'ro', sendActivationEmail: false, initialPassword: '' }); },
      onError: (e: unknown) => setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? t('createFailed')),
    });
  };
  return (
    <FormDialog open={open} title={t('addUser')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField label={t('email')} type="email" required value={form.email} onChange={set('email')} autoFocus />
      <TextField label={t('displayName')} value={form.displayName} onChange={set('displayName')} />
      <TextField label={t('username')} value={form.username} onChange={set('username')} />
      <TextField select label={t('locale')} value={form.locale} onChange={set('locale')}>
        <MenuItem value="ro">Română</MenuItem>
        <MenuItem value="en">English</MenuItem>
      </TextField>
      <TextField label={t('initialPassword')} type="password" value={form.initialPassword} onChange={set('initialPassword')} helperText={t('initialPasswordHelp')} />
      <FormControlLabel control={<Switch checked={form.sendActivationEmail} onChange={(e) => setForm((f) => ({ ...f, sendActivationEmail: e.target.checked }))} />} label={t('sendActivationEmail')} />
    </FormDialog>
  );
}
