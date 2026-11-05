import { appelApi } from "@/lib/client-api";
import type { DashboardAdmin } from "@/types/admin-dashboard";

/**
 * Récupère les données consolidées du tableau de bord administrateur.
 * Appelle l'endpoint GET /admin/dashboard (le préfixe /api est déjà pris en charge par appelApi).
 */
export async function recupererDashboardAdmin(): Promise<DashboardAdmin> {
  return appelApi<DashboardAdmin>("/admin/dashboard");
}

