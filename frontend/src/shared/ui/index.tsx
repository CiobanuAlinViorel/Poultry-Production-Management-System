import { Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

export function PageHeader({ title, subtitle, actions }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 3 }}>
      <Box>
        <Typography variant="h4" component="h1">{title}</Typography>
        {subtitle && <Typography color="text.secondary">{subtitle}</Typography>}
      </Box>
      {actions && <Stack direction="row" spacing={1}>{actions}</Stack>}
    </Stack>
  );
}

export function LoadingBlock() {
  return <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}><CircularProgress /></Box>;
}

export function EmptyState({ message, action }: { message: ReactNode; action?: ReactNode }) {
  return (
    <Stack alignItems="center" spacing={2} sx={{ py: 6, color: 'text.secondary' }}>
      <Typography>{message}</Typography>
      {action}
    </Stack>
  );
}

export function QueryState({ isLoading, isError, isEmpty, emptyMessage, emptyAction, children }:
  { isLoading: boolean; isError: boolean; isEmpty?: boolean; emptyMessage?: ReactNode; emptyAction?: ReactNode; children: ReactNode }) {
  const { t } = useTranslation('errors');
  if (isLoading) return <LoadingBlock />;
  if (isError) return <Alert severity="error">{t('loadFailed')}</Alert>;
  if (isEmpty) return <EmptyState message={emptyMessage ?? t('notFound')} action={emptyAction} />;
  return children;
}

const statusColor: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
  ACTIVE: 'success', PENDING_ACTIVATION: 'info', LOCKED: 'warning', DISABLED: 'error', CLOSED: 'default', true: 'success', false: 'default',
};
export function StatusChip({ value, label }: { value: string | boolean; label?: string }) {
  const key = String(value);
  return <Chip size="small" color={statusColor[key] ?? 'default'} label={label ?? key} />;
}

export function FormDialog({ open, title, onClose, onSubmit, submitting, submitLabel, children, maxWidth = 'sm', error }:
  { open: boolean; title: ReactNode; onClose: () => void; onSubmit: () => void; submitting?: boolean; submitLabel?: ReactNode; children: ReactNode; maxWidth?: 'xs' | 'sm' | 'md'; error?: ReactNode }) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth={maxWidth}>
      <DialogTitle>{title}</DialogTitle>
      <Box component="form" onSubmit={(e) => { e.preventDefault(); onSubmit(); }} noValidate>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            {children}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>{t('cancel')}</Button>
          <Button type="submit" variant="contained" disabled={submitting}>{submitLabel ?? t('save')}</Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export const formatDate = (value?: string | null, withTime = false) => {
  if (!value) return '—';
  const d = new Date(value);
  return withTime ? d.toLocaleString() : d.toLocaleDateString();
};
