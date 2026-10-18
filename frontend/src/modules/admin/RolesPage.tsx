import { Add, Edit } from '@mui/icons-material';
import { Box, Button, Card, CardContent, CardHeader, Checkbox, Chip, FormControlLabel, FormGroup, Grid2 as Grid, IconButton, MenuItem, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Tooltip, Typography } from '@mui/material';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCreatePermission, usePermissions, useRoles, useSaveRole } from '../../shared/api/hooks';
import { SCOPE_TYPES, type Permission, type Role, type RoleRequest, type ScopeType } from '../../shared/api/types';
import { ModuleRoles } from '../../shared/auth/access';
import { useAccess } from '../../shared/auth/store';
import { FormDialog, PageHeader, QueryState, StatusChip } from '../../shared/ui';

const apiMessage = (e: unknown) => (e as { response?: { data?: { message?: string } } }).response?.data?.message;

export function RolesPage() {
  const { t } = useTranslation('admin');
  const { canWrite } = useAccess();
  const writable = canWrite(ModuleRoles.admin);
  const roles = useRoles();
  const permissions = usePermissions();
  const [editing, setEditing] = useState<Role | null | 'new'>(null);
  const [permOpen, setPermOpen] = useState(false);

  return (
    <>
      <PageHeader title={t('rolesTitle')} subtitle={t('rolesPageSubtitle')}
        actions={writable && <>
          <Button variant="outlined" startIcon={<Add />} onClick={() => setPermOpen(true)}>{t('addPermission')}</Button>
          <Button variant="contained" startIcon={<Add />} onClick={() => setEditing('new')}>{t('addRole')}</Button>
        </>} />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card sx={{ overflowX: 'auto' }}>
            <QueryState isLoading={roles.isLoading} isError={roles.isError} isEmpty={roles.data?.length === 0}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>{t('role')}</TableCell>
                    <TableCell>{t('module')}</TableCell>
                    <TableCell>{t('allowedScopes')}</TableCell>
                    <TableCell>{t('permissions')}</TableCell>
                    <TableCell align="right">{t('assignedUsers')}</TableCell>
                    <TableCell>{t('status')}</TableCell>
                    {writable && <TableCell />}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {roles.data?.map((r) => (
                    <TableRow key={r.id} hover>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>{r.code}</Typography>
                        <Typography variant="caption" color="text.secondary">{r.nameRo}{r.systemDefined ? ` · ${t('systemDefined')}` : ''}</Typography>
                      </TableCell>
                      <TableCell>{r.module ?? '—'}</TableCell>
                      <TableCell><Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">{r.allowedScopeTypes.map((s) => <Chip key={s} size="small" label={s} />)}</Stack></TableCell>
                      <TableCell>
                        {r.code === 'SYSTEM_ADMIN'
                          ? <Chip size="small" color="success" label={t('allPermissions')} />
                          : <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">{r.permissions.length ? r.permissions.map((p) => <Chip key={p} size="small" variant="outlined" label={p} />) : <Typography variant="caption" color="text.secondary">—</Typography>}</Stack>}
                      </TableCell>
                      <TableCell align="right">{r.activeAssignments}</TableCell>
                      <TableCell><StatusChip value={r.active} label={r.active ? t('active') : t('inactive')} /></TableCell>
                      {writable && <TableCell align="right"><Tooltip title={t('editRole')}><IconButton size="small" onClick={() => setEditing(r)}><Edit fontSize="small" /></IconButton></Tooltip></TableCell>}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </QueryState>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Card>
            <CardHeader title={t('permissions')} subheader={t('permissionsSubtitle')} />
            <CardContent sx={{ pt: 0 }}>
              <QueryState isLoading={permissions.isLoading} isError={permissions.isError} isEmpty={permissions.data?.length === 0}>
                <Stack spacing={1}>
                  {groupByModule(permissions.data ?? []).map(([module, perms]) => (
                    <Box key={module}>
                      <Typography variant="overline">{module}</Typography>
                      <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">
                        {perms.map((p) => <Tooltip key={p.id} title={p.description ?? ''}><Chip size="small" variant={p.systemDefined ? 'filled' : 'outlined'} label={p.key} /></Tooltip>)}
                      </Stack>
                    </Box>
                  ))}
                </Stack>
              </QueryState>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      {editing && <RoleDialog role={editing === 'new' ? null : editing} permissions={permissions.data ?? []} onClose={() => setEditing(null)} />}
      <PermissionDialog open={permOpen} onClose={() => setPermOpen(false)} />
    </>
  );
}

const groupByModule = (perms: Permission[]) => {
  const map = new Map<string, Permission[]>();
  perms.forEach((p) => { const k = p.module ?? 'OTHER'; map.set(k, [...(map.get(k) ?? []), p]); });
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
};

function RoleDialog({ role, permissions, onClose }: { role: Role | null; permissions: Permission[]; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const save = useSaveRole();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<RoleRequest>(() => (role
    ? { code: role.code, nameRo: role.nameRo, nameEn: role.nameEn ?? '', description: role.description ?? '', module: role.module ?? 'CUSTOM', allowedScopeTypes: role.allowedScopeTypes, permissions: role.permissions, active: role.active }
    : { code: '', nameRo: '', nameEn: '', description: '', module: 'CUSTOM', allowedScopeTypes: ['GLOBAL'], permissions: [], active: true }));
  const grouped = useMemo(() => groupByModule(permissions), [permissions]);
  const toggle = <T,>(list: T[], v: T) => (list.includes(v) ? list.filter((x) => x !== v) : [...list, v]);
  const submit = () => {
    setError(null);
    save.mutate({ id: role?.id, body: { ...form, nameEn: form.nameEn || undefined, description: form.description || undefined } }, {
      onSuccess: onClose,
      onError: (e: unknown) => setError(apiMessage(e) ?? t('createFailed')),
    });
  };
  return (
    <FormDialog open title={role ? t('editRole') : t('addRole')} onClose={onClose} onSubmit={submit} submitting={save.isPending} error={error} maxWidth="md">
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField required label={t('roleCode')} value={form.code} disabled={role?.systemDefined} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} helperText={t('roleCodeHelp')} sx={{ flex: 1 }} />
        <TextField label={t('module')} value={form.module} onChange={(e) => setForm({ ...form, module: e.target.value.toUpperCase() })} sx={{ flex: 1 }} />
      </Stack>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField required label={t('nameRo')} value={form.nameRo} onChange={(e) => setForm({ ...form, nameRo: e.target.value })} sx={{ flex: 1 }} />
        <TextField label={t('nameEn')} value={form.nameEn} onChange={(e) => setForm({ ...form, nameEn: e.target.value })} sx={{ flex: 1 }} />
      </Stack>
      <TextField label={t('description')} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} multiline minRows={2} />
      <TextField select label={t('status')} value={form.active ? 'true' : 'false'} onChange={(e) => setForm({ ...form, active: e.target.value === 'true' })} sx={{ maxWidth: 240 }}>
        <MenuItem value="true">{t('active')}</MenuItem>
        <MenuItem value="false">{t('inactive')}</MenuItem>
      </TextField>
      <Box>
        <Typography variant="subtitle2" gutterBottom>{t('allowedScopes')}</Typography>
        <FormGroup row>
          {SCOPE_TYPES.map((s: ScopeType) => <FormControlLabel key={s} control={<Checkbox checked={form.allowedScopeTypes.includes(s)} onChange={() => setForm({ ...form, allowedScopeTypes: toggle(form.allowedScopeTypes, s) })} />} label={s === 'GLOBAL' ? t('scopeGlobal') : t(`scopeType.${s}`)} />)}
        </FormGroup>
      </Box>
      <Box>
        <Typography variant="subtitle2" gutterBottom>{t('permissions')}</Typography>
        {form.code === 'SYSTEM_ADMIN' ? <Typography variant="body2" color="text.secondary">{t('systemAdminAllPermissions')}</Typography> : grouped.map(([module, perms]) => (
          <Box key={module} sx={{ mb: 1 }}>
            <Typography variant="overline">{module}</Typography>
            <FormGroup row>
              {perms.map((p) => <FormControlLabel key={p.id} control={<Checkbox size="small" checked={form.permissions.includes(p.key)} onChange={() => setForm({ ...form, permissions: toggle(form.permissions, p.key) })} />} label={p.key} />)}
            </FormGroup>
          </Box>
        ))}
      </Box>
    </FormDialog>
  );
}

function PermissionDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const create = useCreatePermission();
  const [key, setKey] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    setError(null);
    create.mutate({ key, description: description || undefined }, {
      onSuccess: () => { onClose(); setKey(''); setDescription(''); },
      onError: (e: unknown) => setError(apiMessage(e) ?? t('createFailed')),
    });
  };
  return (
    <FormDialog open={open} title={t('addPermission')} onClose={onClose} onSubmit={submit} submitting={create.isPending} error={error}>
      <TextField required label={t('permissionKey')} value={key} onChange={(e) => setKey(e.target.value.toLowerCase())} helperText={t('permissionKeyHelp')} autoFocus />
      <TextField label={t('description')} value={description} onChange={(e) => setDescription(e.target.value)} />
    </FormDialog>
  );
}
