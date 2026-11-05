import type { CompteBancaire, Virement } from "@/types/banque";

/** Réponse renvoyée par GET /api/client/dashboard. */
export interface DashboardClient {
  comptes: CompteBancaire[];
  derniersVirements: Virement[];
  nombreReclamationsOuvertes: number;
}
