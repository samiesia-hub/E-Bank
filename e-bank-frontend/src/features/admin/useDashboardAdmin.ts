import { useQuery } from "@tanstack/react-query";
import { recupererDashboardAdmin } from "@/lib/api/admin-dashboard";
import type { DashboardAdmin } from "@/types/admin-dashboard";

/**
 * Hook personnalisé React Query pour récupérer les données du tableau de bord administrateur.
 * Clé de cache unique partagée entre les compteurs et les activités récentes (une seule requête réseau).
 */
export function useDashboardAdmin() {
  return useQuery<DashboardAdmin>({
    queryKey: ["admin", "dashboard"],
    queryFn: recupererDashboardAdmin,
  });
}

