import { QueryClient, QueryClientProvider as TanStackQueryClientProvider } from '@tanstack/react-query';
import type { PropsWithChildren } from 'react';

const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1 } } });

export function QueryClientProvider({ children }: PropsWithChildren) {
  return <TanStackQueryClientProvider client={queryClient}>{children}</TanStackQueryClientProvider>;
}
