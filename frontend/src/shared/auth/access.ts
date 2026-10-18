// Mirrors backend ScopeGuard semantics (shared-kernel ScopeGuard.canRead / canWrite):
// SYSTEM_ADMIN can do everything, MANAGER is read-only everywhere.

export const Roles = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  MANAGER: 'MANAGER',
  HR_ADMIN: 'HR_ADMIN',
  FLOCK_SUPERVISOR: 'FLOCK_SUPERVISOR',
  FARM_MANAGER: 'FARM_MANAGER',
  HATCHERY_SUPERVISOR: 'HATCHERY_SUPERVISOR',
  SLAUGHTER_MANAGER: 'SLAUGHTER_MANAGER',
} as const;

export type Role = (typeof Roles)[keyof typeof Roles];

export const ModuleRoles = {
  admin: [] as Role[],
  workforce: [Roles.HR_ADMIN],
  parentFlock: [Roles.FLOCK_SUPERVISOR],
  hatchery: [Roles.HATCHERY_SUPERVISOR],
  farm: [Roles.FARM_MANAGER],
  slaughterhouse: [Roles.SLAUGHTER_MANAGER],
  reporting: [Roles.HR_ADMIN, Roles.FLOCK_SUPERVISOR, Roles.FARM_MANAGER, Roles.HATCHERY_SUPERVISOR, Roles.SLAUGHTER_MANAGER],
} as const;

const hasAny = (userRoles: readonly string[], roles: readonly string[]) => roles.some((r) => userRoles.includes(r));

export const isSystemAdmin = (userRoles: readonly string[]) => userRoles.includes(Roles.SYSTEM_ADMIN);

export const canRead = (userRoles: readonly string[], roles: readonly string[]) =>
  isSystemAdmin(userRoles) || userRoles.includes(Roles.MANAGER) || hasAny(userRoles, roles);

export const canWrite = (userRoles: readonly string[], roles: readonly string[]) =>
  isSystemAdmin(userRoles) || hasAny(userRoles, roles);

interface JwtClaims { sub?: string; username?: string; name?: string; roles?: { role: string }[] }

export interface TokenUser { id: string; email: string; displayName?: string; roles: string[] }

/** Fallback when the auth response carries no `user`: derive identity and role codes from the access token. */
export const userFromToken = (accessToken: string): TokenUser | null => {
  try {
    const payload = accessToken.split('.')[1] ?? '';
    const claims = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as JwtClaims;
    return {
      id: claims.sub ?? '',
      email: claims.username ?? '',
      displayName: claims.name,
      roles: [...new Set((claims.roles ?? []).map((c) => c.role))],
    };
  } catch { return null; }
};
