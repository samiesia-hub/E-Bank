import { appelApi, messageErreurApi } from "@/lib/client-api";
import type { StatutCompte, TypeCompte } from "@/types/banque";

/**
 * Compte tel que renvoyé par /api/admin/comptes.
 * Le backend expose le RIB (format marocain 24 chiffres) et le solde en BigDecimal,
 * que JSON transporte en nombre : l'affichage repasse par Intl.NumberFormat.
 */
export interface CompteAdmin {
  id: number;
  numeroCompte: string;
  rib: string;
  devise: string;
  solde: number;
  typeCompte: TypeCompte;
  statutCompte: StatutCompte;
  dateCreation: string;
  clientId: number | null;
  clientEmail: string | null;
  clientNom: string | null;
}

export interface OuvertureComptePayload {
  clientId: number;
  typeCompte: TypeCompte;
  /** RIB optionnel : généré par le backend s'il est absent. */
  rib?: string;
  /** Solde initial en MAD (0 par défaut). */
  soldeInitial?: number;
}

/** Liste de tous les comptes de la banque (ADMIN). */
export function listerComptesAdmin(): Promise<CompteAdmin[]> {
  return appelApi<CompteAdmin[]>("/admin/comptes");
}

/** Ouvre un compte pour un client existant ; RIB généré si non fourni. */
export function creerCompte(payload: OuvertureComptePayload): Promise<CompteAdmin> {
  return appelApi<CompteAdmin>("/admin/comptes", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/** Gele (BLOQUE) ou réactive (ACTIF) un compte client. */
export function changerStatutCompte(id: number, statut: StatutCompte): Promise<CompteAdmin> {
  return appelApi<CompteAdmin>(`/admin/comptes/${id}/statut`, {
    method: "PATCH",
    body: JSON.stringify({ statut }),
  });
}

/** Alias historique : le helper vit désormais dans client-api. */
export const messageErreur = messageErreurApi;
