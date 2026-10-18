import type { PropsWithChildren } from 'react';
import { ApiErrorProvider } from './ApiErrorProvider';
import { AuthProvider } from './AuthProvider';
import { I18nProvider } from './I18nProvider';
import { QueryClientProvider } from './QueryClientProvider';
import { ThemeProvider } from './ThemeProvider';

export function AppProviders({ children }: PropsWithChildren) {
  return <I18nProvider><ThemeProvider><QueryClientProvider><ApiErrorProvider><AuthProvider>{children}</AuthProvider></ApiErrorProvider></QueryClientProvider></ThemeProvider></I18nProvider>;
}
