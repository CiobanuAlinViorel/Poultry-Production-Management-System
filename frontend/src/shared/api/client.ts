import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { getRefreshToken, getDeviceId, useAuthStore, type AuthUser } from '../auth/store';
import { getErrorCode } from './errors';
import { notifyApiError } from './notifications';

const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  expiresIn: number;
  user?: AuthUser;
  mfaRequired?: boolean;
  mfaToken?: string;
}

export const apiClient = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } });
const refreshClient = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } });
let refreshPromise: Promise<string> | null = null;

export const refreshSession = async (): Promise<string> => {
  if (refreshPromise) return refreshPromise;
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token');
  refreshPromise = refreshClient
    .post<AuthResponse>('/api/identity/auth/refresh', { refreshToken, deviceId: getDeviceId() })
    .then(({ data }) => {
      useAuthStore.getState().setSession(data.accessToken, data.expiresIn, data.refreshToken, data.user);
      return data.accessToken;
    })
    .catch((error: unknown) => {
      useAuthStore.getState().clearSession();
      throw error;
    })
    .finally(() => { refreshPromise = null; });
  return refreshPromise;
};

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

interface RetryConfig extends InternalAxiosRequestConfig { _retry?: boolean }
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined;
    if (error.response?.status === 401 && config && !config._retry && getRefreshToken()) {
      config._retry = true;
      try {
        config.headers.Authorization = `Bearer ${await refreshSession()}`;
        return await apiClient(config);
      } catch {
        window.location.assign('/login');
      }
    }
    notifyApiError(getErrorCode(error.response?.data));
    return Promise.reject(error);
  },
);
