import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, Skeleton, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { apiClient } from '../../shared/api/client';

interface UserDetail { id: string; displayName?: string; email: string; status: string; roles: string[] }
export function UserDetailPage() {
  const { t } = useTranslation('admin');
  const { id } = useParams();
  const { data, isLoading } = useQuery({ queryKey: ['admin', 'users', id], queryFn: async () => (await apiClient.get<UserDetail>(`/api/admin/users/${id}`)).data, enabled: Boolean(id) });
  return <><Typography variant="h4" gutterBottom>{t('userDetails')}</Typography>{isLoading ? <Skeleton height={180} /> : <Card><CardContent><Typography variant="h6">{data?.displayName ?? data?.email}</Typography><Typography>{data?.email}</Typography><Typography>{t('status')}: {data?.status}</Typography><Typography>{t('roles')}: {data?.roles.join(', ')}</Typography><Typography color="text.secondary" sx={{ mt: 2 }}>{t('userId')}: {data?.id}</Typography></CardContent></Card>}</>;
}
