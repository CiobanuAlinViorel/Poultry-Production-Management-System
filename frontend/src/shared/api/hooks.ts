import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  Assignment, AssignmentRequest, Breed, CreateUserRequest, DailySheet, DailySheetRequest, Farm, FarmDelivery,
  FarmDeliveryRequest, FarmRequest, House, HouseRequest, Lot, LotRequest, PageResponse, Permission, PermissionRequest,
  Role, RoleRequest, ScopeType, Silo, SiloRequest, Site, SiteSummary, SpringPage, User, UserStatus,
} from './types';

const get = async <T,>(url: string, params?: Record<string, unknown>) => (await apiClient.get<T>(url, { params })).data;
const post = async <T,>(url: string, body?: unknown) => (await apiClient.post<T>(url, body)).data;
const put = async <T,>(url: string, body?: unknown) => (await apiClient.put<T>(url, body)).data;
const del = async (url: string) => { await apiClient.delete(url); };

const useInvalidate = () => {
  const qc = useQueryClient();
  return (...keys: unknown[][]) => Promise.all(keys.map((queryKey) => qc.invalidateQueries({ queryKey })));
};

// ------------------------------------------------------------------ identity users
export interface UserListParams { page: number; size: number; q?: string; status?: UserStatus | '' }
export const useUsers = (params: UserListParams, enabled = true) =>
  useQuery({
    enabled,
    queryKey: ['identity', 'users', params],
    queryFn: () => get<SpringPage<User>>('/api/identity/users', { page: params.page, size: params.size, q: params.q || undefined, status: params.status || undefined, sort: 'createdAt,desc' }),
  });
export const useUser = (id?: string) =>
  useQuery({ queryKey: ['identity', 'users', id], queryFn: () => get<User>(`/api/identity/users/${id}`), enabled: Boolean(id) });
export const useCreateUser = () => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: CreateUserRequest) => post<User>('/api/identity/users', body), onSuccess: () => invalidate(['identity', 'users']) });
};
export type UserAction = 'enable' | 'disable' | 'unlock' | 'reset-password' | 'force-password-change' | 'mfa/reset';
export const useUserAction = (id: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (action: UserAction) => post<void>(`/api/identity/users/${id}/${action}`), onSuccess: () => invalidate(['identity', 'users']) });
};

// ------------------------------------------------------------------ admin / access
export const useRoles = (enabled = true) => useQuery({ queryKey: ['access', 'roles'], queryFn: () => get<Role[]>('/api/admin/access/roles'), enabled });
export const usePermissions = () => useQuery({ queryKey: ['access', 'permissions'], queryFn: () => get<Permission[]>('/api/admin/access/permissions') });
export const useSites = (scopeType?: ScopeType) =>
  useQuery({ queryKey: ['access', 'sites', scopeType], queryFn: () => get<Site[]>('/api/admin/access/sites', { scopeType }), enabled: scopeType !== 'GLOBAL' });
export const useSaveRole = () => {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: RoleRequest }) => (id ? put<Role>(`/api/admin/access/roles/${id}`, body) : post<Role>('/api/admin/access/roles', body)),
    onSuccess: () => invalidate(['access', 'roles']),
  });
};
export const useSetRolePermissions = () => {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, permissions }: { id: string; permissions: string[] }) => put<Role>(`/api/admin/access/roles/${id}/permissions`, { permissions }),
    onSuccess: () => invalidate(['access', 'roles']),
  });
};
export const useCreatePermission = () => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: PermissionRequest) => post<Permission>('/api/admin/access/permissions', body), onSuccess: () => invalidate(['access', 'permissions']) });
};
export const useAssignments = (userId?: string, includeInactive = false) =>
  useQuery({ queryKey: ['access', 'assignments', userId, includeInactive], queryFn: () => get<Assignment[]>('/api/admin/access/assignments', { userId, includeInactive }), enabled: Boolean(userId) });
export const useAssignmentsByUsers = (userIds: string[]) =>
  useQuery({
    queryKey: ['access', 'assignments', 'by-users', userIds],
    queryFn: () => get<Record<string, Assignment[]>>('/api/admin/access/assignments/by-users', { userIds: userIds.join(',') }),
    enabled: userIds.length > 0,
  });
export const useAssignRole = () => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: AssignmentRequest) => post<Assignment>('/api/admin/access/assignments', body), onSuccess: () => invalidate(['access', 'assignments'], ['access', 'roles']) });
};
export const useRevokeAssignment = () => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (id: string) => del(`/api/admin/access/assignments/${id}`), onSuccess: () => invalidate(['access', 'assignments'], ['access', 'roles']) });
};

// ------------------------------------------------------------------ catalog
export const useBreeds = () =>
  useQuery({ queryKey: ['catalog', 'breeds'], queryFn: async () => (await get<PageResponse<Breed>>('/api/admin/breeds', { size: 200, activeOnly: true })).content });

// ------------------------------------------------------------------ farm
export const useFarms = (enabled = true) => useQuery({ queryKey: ['farm', 'farms'], queryFn: () => get<Farm[]>('/api/farm/farms'), enabled });
export const useFarm = (farmId?: string) => useQuery({ queryKey: ['farm', 'farms', farmId], queryFn: () => get<Farm>(`/api/farm/farms/${farmId}`), enabled: Boolean(farmId) });
export const useCreateFarm = () => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: FarmRequest) => post<Farm>('/api/farm/farms', body), onSuccess: () => invalidate(['farm', 'farms']) });
};
export const useHouses = (farmId?: string) => useQuery({ queryKey: ['farm', farmId, 'houses'], queryFn: () => get<House[]>(`/api/farm/farms/${farmId}/houses`), enabled: Boolean(farmId) });
export const useCreateHouse = (farmId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: HouseRequest) => post<House>(`/api/farm/farms/${farmId}/houses`, body), onSuccess: () => invalidate(['farm', farmId, 'houses']) });
};
export const useSilos = (farmId?: string) => useQuery({ queryKey: ['farm', farmId, 'silos'], queryFn: () => get<Silo[]>(`/api/farm/farms/${farmId}/silos`), enabled: Boolean(farmId) });
export const useCreateSilo = (farmId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: SiloRequest) => post<Silo>(`/api/farm/farms/${farmId}/silos`, body), onSuccess: () => invalidate(['farm', farmId, 'silos']) });
};
export const useLots = (farmId?: string) => useQuery({ queryKey: ['farm', farmId, 'lots'], queryFn: () => get<Lot[]>(`/api/farm/farms/${farmId}/lots`), enabled: Boolean(farmId) });
export const useCreateLot = (farmId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: LotRequest) => post<Lot>(`/api/farm/farms/${farmId}/lots`, body), onSuccess: () => invalidate(['farm', farmId, 'lots']) });
};
export const useCloseLot = (farmId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (lotId: string) => post<Lot>(`/api/farm/lots/${lotId}/close`), onSuccess: () => invalidate(['farm', farmId, 'lots']) });
};
export const useDailySheets = (lotId?: string) => useQuery({ queryKey: ['farm', 'lots', lotId, 'sheets'], queryFn: () => get<DailySheet[]>(`/api/farm/lots/${lotId}/daily-sheets`), enabled: Boolean(lotId) });
export const useRecordDailySheet = (lotId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: DailySheetRequest) => post<DailySheet>(`/api/farm/lots/${lotId}/daily-sheets`, body), onSuccess: () => invalidate(['farm', 'lots', lotId, 'sheets']) });
};
export const useDeliveries = (lotId?: string) => useQuery({ queryKey: ['farm', 'lots', lotId, 'deliveries'], queryFn: () => get<FarmDelivery[]>(`/api/farm/lots/${lotId}/deliveries`), enabled: Boolean(lotId) });
export const useRecordDelivery = (lotId: string) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: FarmDeliveryRequest) => post<FarmDelivery>(`/api/farm/lots/${lotId}/deliveries`, body), onSuccess: () => invalidate(['farm', 'lots', lotId, 'deliveries']) });
};

// ------------------------------------------------------------------ other site modules
export type SiteModule = 'parent-flock' | 'hatchery' | 'slaughterhouse';
export const useModuleSites = (module: SiteModule, enabled = true) =>
  useQuery({ queryKey: [module, 'sites'], queryFn: () => get<SiteSummary[]>(`/api/${module}/sites`), enabled });
export const useCreateModuleSite = (module: SiteModule) => {
  const invalidate = useInvalidate();
  return useMutation({ mutationFn: (body: Record<string, unknown>) => post<SiteSummary>(`/api/${module}/sites`, body), onSuccess: () => invalidate([module, 'sites']) });
};
