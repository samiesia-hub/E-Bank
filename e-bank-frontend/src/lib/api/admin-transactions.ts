import { BASE_API, ErreurApi, appelApi } from "@/lib/client-api";
import type { StatutTransaction } from "@/types/banque";

export type SensTransaction = "DEBIT" | "CREDIT";

/** Transaction telle que renvoyée par /api/admin/transactions. */
export interface TransactionAdmin {
  id: number;
  dateTransaction: string;
  montant: number;
  devise: string | null;
  sens: SensTransaction | null;
  statutTransaction: StatutTransaction | null;
  libelle: string | null;
  compteId: number | null;
  numeroCompte: string | null;
  clientId: number | null;
  emetteur: string | null;
  destinataire: string | null;
}

/** Enveloppe PageResponse du backend. */
export interface PageTransactionsAdmin {
  contenu: TransactionAdmin[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
  dernierePage: boolean;
}

export interface FiltresTransactions {
  clientId?: number;
  compteId?: number;
  /** Date ISO AAAA-MM-JJ, incluse. */
  du?: string;
  /** Date ISO AAAA-MM-JJ, incluse. */
  au?: string;
  sens?: SensTransaction;
  page?: number;
  size?: number;
}

function parametres(filtres: FiltresTransactions): URLSearchParams {
  const params = new URLSearchParams();
  if (filtres.clientId != null) params.set("clientId", String(filtres.clientId));
  if (filtres.compteId != null) params.set("compteId", String(filtres.compteId));
  if (filtres.du) params.set("du", filtres.du);
  if (filtres.au) params.set("au", filtres.au);
  if (filtres.sens) params.set("sens", filtres.sens);
  if (filtres.page != null) params.set("page", String(filtres.page));
  if (filtres.size != null) params.set("size", String(filtres.size));
  return params;
}

/** Supervision globale : page de transactions filtrable. */
export function rechercherTransactionsAdmin(
  filtres: FiltresTransactions = {}
): Promise<PageTransactionsAdmin> {
  const params = parametres(filtres);
  const requete = params.size > 0 ? `?${params}` : "";
  return appelApi<PageTransactionsAdmin>(`/admin/transactions${requete}`);
}

/**
 * Télécharge le CSV des transactions correspondant aux filtres affichés
 * (sans pagination : l'export couvre tout le résultat filtré).
 * On passe par fetch pour garder le cookie httpOnly et afficher les erreurs 400/403.
 */
export async function exporterTransactionsCsv(filtres: FiltresTransactions = {}): Promise<string> {
  const params = parametres({ ...filtres, page: undefined, size: undefined });
  const requete = params.size > 0 ? `?${params}` : "";
  const res = await fetch(`${BASE_API}/admin/transactions/export-csv${requete}`, {
    credentials: "include",
  });

  if (res.status === 401 && typeof window !== "undefined") {
    window.location.assign("/connexion");
    throw new ErreurApi(401, "Session expirée — veuillez vous reconnecter.");
  }

  if (!res.ok) {
    const corps = await res.text().catch(() => "");
    throw new ErreurApi(res.status, corps || `Erreur API (${res.status})`);
  }

  const nomFichier = nomDepuisDisposition(res.headers.get("content-disposition"));
  const blob = await res.blob();

  if (typeof window !== "undefined") {
    const url = URL.createObjectURL(blob);
    const lien = document.createElement("a");
    lien.href = url;
    lien.download = nomFichier;
    document.body.appendChild(lien);
    lien.click();
    lien.remove();
    URL.revokeObjectURL(url);
  }

  return nomFichier;
}

/** "attachment; filename=transactions-2026-11-06.csv" -> nom de fichier. */
function nomDepuisDisposition(disposition: string | null): string {
  if (!disposition) return "transactions.csv";
  const capture = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  if (!capture) return "transactions.csv";
  try {
    return decodeURIComponent(capture[1]);
  } catch {
    return capture[1];
  }
}
