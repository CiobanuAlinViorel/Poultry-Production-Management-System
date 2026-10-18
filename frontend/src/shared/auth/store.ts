import { create } from 'zustand';
import { canRead, canWrite, isSystemAdmin, userFromToken } from './access';

const REFRESH_TOKEN_KEY = 'ppms.refreshToken';
const DEVICE_ID_KEY = 'ppms.deviceId';
const VIEW_AS_KEY = 'ppms.viewAsRole';

export interface AuthUser {
  id: string;
  email: string;
  displayName?: string;
  roles: string[];
}

interface AuthState {
  accessToken: string | null;
  accessTokenExpiresAt: number | null;
  user: AuthUser | null;
  initialized: boolean;
  /** SYSTEM_ADMIN only: preview the UI as if the user had just this role (UI-side only, the API still sees the admin). */
  viewAsRole: string | null;
  setSession: (accessToken: string, expiresIn: number, refreshToken?: string, user?: AuthUser) => void;
  setInitialized: (initialized: boolean) => void;
  setViewAsRole: (role: string | null) => void;
  clearSession: () => void;
}

export const getDeviceId = (): string => {
  let id = localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
};

export const getRefreshToken = (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY);

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  accessTokenExpiresAt: null,
  user: null,
  initialized: false,
  viewAsRole: sessionStorage.getItem(VIEW_AS_KEY),
  setSession: (accessToken, expiresIn, refreshToken, user) => {
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    set((state) => {
      const fromToken = userFromToken(accessToken);
      const resolved = user ?? state.user ?? fromToken;
      const roles = resolved?.roles?.length ? resolved.roles : (fromToken?.roles ?? []);
      return {
        accessToken,
        accessTokenExpiresAt: Date.now() + expiresIn * 1000,
        user: resolved ? { ...resolved, roles } : null,
        initialized: true,
      };
    });
  },
  setInitialized: (initialized) => set({ initialized }),
  setViewAsRole: (role) => {
    if (role) sessionStorage.setItem(VIEW_AS_KEY, role); else sessionStorage.removeItem(VIEW_AS_KEY);
    set({ viewAsRole: role });
  },
  clearSession: () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(VIEW_AS_KEY);
    set({ accessToken: null, accessTokenExpiresAt: null, user: null, viewAsRole: null, initialized: true });
  },
}));

const EMPTY_ROLES: readonly string[] = [];

/**
 * Role-based access helpers bound to the current user (mirrors backend ScopeGuard).
 * When a SYSTEM_ADMIN previews another role, `roles` reflects the previewed role while
 * `realRoles` / `realIsSystemAdmin` keep describing the actual account.
 */
export const useAccess = () => {
  const realRoles = useAuthStore((state) => state.user?.roles ?? EMPTY_ROLES);
  const viewAsRole = useAuthStore((state) => state.viewAsRole);
  const realIsSystemAdmin = isSystemAdmin(realRoles);
  const previewing = realIsSystemAdmin && viewAsRole !== null && viewAsRole !== '';
  const roles: readonly string[] = previewing ? [viewAsRole] : realRoles;
  return {
    roles,
    realRoles,
    realIsSystemAdmin,
    viewAsRole: previewing ? viewAsRole : null,
    isSystemAdmin: isSystemAdmin(roles),
    canRead: (moduleRoles: readonly string[]) => canRead(roles, moduleRoles),
    canWrite: (moduleRoles: readonly string[]) => canWrite(roles, moduleRoles),
  };
};
