export interface ApiErrorEnvelope {
  code?: string;
  message?: string;
  details?: unknown;
}

export const getErrorCode = (data: unknown): string => {
  if (typeof data === 'object' && data !== null && 'code' in data && typeof data.code === 'string') return data.code;
  return 'unexpected';
};
