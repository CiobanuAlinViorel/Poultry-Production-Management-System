type ErrorListener = (code: string) => void;
const listeners = new Set<ErrorListener>();

export const onApiError = (listener: ErrorListener): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const notifyApiError = (code: string): void => listeners.forEach((listener) => listener(code));
