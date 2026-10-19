"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

/**
 * Global providers: TanStack Query for server state
 * (accounts, transactions, tickets). Backend enforced RBAC required —
 * UI hiding is NOT security (see src/proxy.ts).
 */
export function Fournisseurs({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

export { Fournisseurs as Providers };
