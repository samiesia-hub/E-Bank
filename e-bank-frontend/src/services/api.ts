/**
 * services/api.ts — couche d'appel REST pour l'espace client.
 * La session est un cookie httpOnly (jamais localStorage).
 * Le 401 déclenche une redirection vers /connexion.
 */
import { appelApi } from "@/lib/client-api";
import type { CompteBancaire } from "@/types/banque";

// ─── Types miroir du backend ──────────────────────────────────────────────────

/** Réponse de GET /api/client/comptes (CompteResponse.java) */
export interface CompteClient {
  id: number;
  numeroCompte: string;
  rib: string;
  devise: string;
  solde: number;          // BigDecimal sérialisé en nombre JSON
  typeCompte: CompteBancaire["type"];
  statutCompte: CompteBancaire["statut"];
  dateCreation: string;
  clientId: number | null;
  clientEmail: string | null;
  clientNom: string | null;
}

/** Un élément de réponse paginée (PageResponse<TransactionResponse>) */
export interface TransactionClient {
  id: number;
  montant: number;        // BigDecimal côté Java → number JSON
  sens: "CREDIT" | "DEBIT";
  libelle: string;
  dateTransaction: string;
  statut: string;
  compteId: number;
  compteNumero: string;
}

/** Réponse paginée générique */
export interface PageClient<T> {
  contenu: T[];
  page: number;
  taille: number;
  total: number;
  totalPages: number;
}

/** Réponse de GET /api/client/dashboard (DashboardClientResponse.java) */
export interface DashboardClientApi {
  comptes: Array<{
    id: number;
    numeroCompte: string;
    typeCompte: string;
    solde: number;
    statutCompte: string;
  }>;
  derniersVirements: Array<{
    id: number;
    dateTransaction: string;
    montant: number;
    beneficiaire: string;
    motif: string;
    statut: string;
  }>;
  nombreReclamationsOuvertes: number;
}

/** Réponse de GET /api/client/profile (ProfilClientResponse.java) */
export interface ProfilClient {
  id: number;
  numeroClient: string;
  nom: string;
  email: string;
  telephone: string | null;
  adresse: string | null;
}

// ─── Fonctions d'appel ────────────────────────────────────────────────────────

/** Tableau de bord : comptes résumés + derniers virements + nb réclamations. */
export function obtenirDashboard(): Promise<DashboardClientApi> {
  return appelApi<DashboardClientApi>("/client/dashboard");
}

/** Liste des comptes du client connecté. */
export function obtenirMesComptes(): Promise<CompteClient[]> {
  return appelApi<CompteClient[]>("/client/comptes");
}

/** Transactions du client, paginées (tous comptes ou un compte précis). */
export function obtenirMesTransactions(params?: {
  compteId?: number;
  page?: number;
  size?: number;
}): Promise<PageClient<TransactionClient>> {
  const qs = new URLSearchParams();
  if (params?.compteId != null) qs.set("compteId", String(params.compteId));
  if (params?.page != null) qs.set("page", String(params.page));
  if (params?.size != null) qs.set("size", String(params.size));
  const query = qs.toString() ? `?${qs}` : "";
  return appelApi<PageClient<TransactionClient>>(`/client/transactions${query}`);
}

/** Transactions d'un compte précis, paginées. */
export function obtenirTransactionsDuCompte(
  compteId: number,
  params?: { page?: number; size?: number }
): Promise<PageClient<TransactionClient>> {
  const qs = new URLSearchParams();
  if (params?.page != null) qs.set("page", String(params.page));
  if (params?.size != null) qs.set("size", String(params.size));
  const query = qs.toString() ? `?${qs}` : "";
  return appelApi<PageClient<TransactionClient>>(
    `/comptes/${compteId}/transactions${query}`
  );
}

/** Profil du client connecté. */
export function obtenirMonProfil(): Promise<ProfilClient> {
  return appelApi<ProfilClient>("/client/profile");
}

// ─── Réclamations ─────────────────────────────────────────────────────────────

export interface ReclamationClient {
  id: number;
  objet: string;
  message: string;
  statut: string; // OUVERTE | EN_COURS | RESOLUE | FERMEE
  dateCreation: string;
}

export interface CreerReclamationPayload {
  objet: string;
  message: string;
}

/** Liste des réclamations du client connecté. */
export function obtenirMesReclamations(): Promise<ReclamationClient[]> {
  return appelApi<ReclamationClient[]>("/client/reclamations");
}

/** Détail d'une réclamation. */
export function obtenirReclamation(id: number): Promise<ReclamationClient> {
  return appelApi<ReclamationClient>(`/client/reclamations/${id}`);
}

/** Créer une réclamation. */
export function creerReclamation(
  payload: CreerReclamationPayload
): Promise<ReclamationClient> {
  return appelApi<ReclamationClient>("/client/reclamations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

