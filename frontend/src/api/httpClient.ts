import axios, { type InternalAxiosRequestConfig } from 'axios';
import { getAuth, setAuth } from '../auth/tokenStore';
import type { LoginResponse } from '../types/auth';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export const httpClient = axios.create({ baseURL: API_BASE_URL });

httpClient.interceptors.request.use((config) => {
  const auth = getAuth();
  if (auth) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let refreshing: Promise<string> | null = null;

async function refrescarToken(refreshToken: string): Promise<string> {
  if (!refreshing) {
    refreshing = axios
      .post<LoginResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken })
      .then(({ data }) => {
        const actual = getAuth();
        if (actual) {
          setAuth({ ...actual, accessToken: data.accessToken, refreshToken: data.refreshToken });
        }
        return data.accessToken;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as RetriableConfig | undefined;
    const auth = getAuth();

    if (error.response?.status === 401 && auth && original && !original._retry) {
      original._retry = true;
      try {
        const accessToken = await refrescarToken(auth.refreshToken);
        original.headers.Authorization = `Bearer ${accessToken}`;
        return httpClient(original);
      } catch {
        setAuth(null);
      }
    }
    return Promise.reject(error);
  },
);
