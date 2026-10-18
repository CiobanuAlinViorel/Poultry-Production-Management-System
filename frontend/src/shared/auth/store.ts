import { create } from 'zustand';
import { canRead, canWrite, isSystemAdmin, userFromToken } from './access';

const REFRESH_TOKEN_KEY = 'ppms.refreshToken';
const DEVICE_ID_KEY = 'ppms.deviceId';

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
  setSession: (accessToken: string, expiresIn: number, refreshToken?: string, user?: AuthUser) => void;
  setInitialized: (initialized: boolean) => void;
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
  clearSession: () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    set({ accessToken: null, accessTokenExpiresAt: null, user: null, initialized: true });
  },
}));

const EMPTY_ROLES: readonly string[] = [];

/** Role-based access helpers bound to the current user (mirrors backend ScopeGuard). */
export const useAccess = () => {
  const roles = useAuthStore((state) => state.user?.roles ?? EMPTY_ROLES);
  return {
    roles,
    isSystemAdmin: isSystemAdmin(roles),
    canRead: (moduleRoles: readonly string[]) => canRead(roles, moduleRoles),
    canWrite: (moduleRoles: readonly string[]) => canWrite(roles, moduleRoles),
  };
};
