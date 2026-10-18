import { useQuery } from '@tanstack/react-query';
import { Alert, Card, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { apiClient } from '../../shared/api/client';

interface UserSummary { id: string; displayName?: string; email: string; status: string; roles: string[] }

export function UsersPage() {
  const { t } = useTranslation('admin');
  const query = useQuery({ queryKey: ['admin', 'users'], queryFn: async () => (await apiClient.get<UserSummary[] | { content: UserSummary[] }>('/api/admin/users')).data });
  const users = Array.isArray(query.data) ? query.data : query.data?.content ?? [];
  return <><Typography variant="h4" gutterBottom>{t('usersTitle')}</Typography>{query.isError && <Alert severity="error">{t('noUsers')}</Alert>}<Card sx={{ overflowX: 'auto' }}><Table><TableHead><TableRow><TableCell>{t('name')}</TableCell><TableCell>{t('email')}</TableCell><TableCell>{t('status')}</TableCell><TableCell>{t('roles')}</TableCell></TableRow></TableHead><TableBody>{users.map((user) => <TableRow key={user.id} hover><TableCell><Link to={`/admin/users/${user.id}`}>{user.displayName ?? user.email}</Link></TableCell><TableCell>{user.email}</TableCell><TableCell>{user.status}</TableCell><TableCell>{user.roles.join(', ')}</TableCell></TableRow>)}</TableBody></Table></Card></>;
}
