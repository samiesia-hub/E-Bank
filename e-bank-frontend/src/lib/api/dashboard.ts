import { appelApi } from "@/lib/client-api";
import type { DashboardClient } from "@/types/dashboard";

/** Charge les informations personnelles du tableau de bord du client connecté. */
export function obtenirDashboardClient(): Promise<DashboardClient> {
  return appelApi<DashboardClient>("/client/dashboard");
}
