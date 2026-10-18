import { create } from 'zustand';

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
    set((state) => ({
      accessToken,
      accessTokenExpiresAt: Date.now() + expiresIn * 1000,
      user: user ?? state.user,
      initialized: true,
    }));
  },
  setInitialized: (initialized) => set({ initialized }),
  clearSession: () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    set({ accessToken: null, accessTokenExpiresAt: null, user: null, initialized: true });
  },
}));
