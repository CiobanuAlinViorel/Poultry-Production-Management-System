import { Alert, Snackbar } from '@mui/material';
import { useEffect, useState, type PropsWithChildren } from 'react';
import { useTranslation } from 'react-i18next';
import { onApiError } from '../../shared/api/notifications';

export function ApiErrorProvider({ children }: PropsWithChildren) {
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const { t } = useTranslation('errors');
  useEffect(() => onApiError(setErrorCode), []);
  return <>{children}<Snackbar open={Boolean(errorCode)} autoHideDuration={5000} onClose={() => setErrorCode(null)}><Alert severity="error" onClose={() => setErrorCode(null)}>{errorCode ? t(errorCode, { defaultValue: t('unexpected') }) : ''}</Alert></Snackbar></>;
}
