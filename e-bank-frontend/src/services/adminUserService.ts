import { appelApi, messageErreurApi } from "@/lib/client-api";

export type RoleUtilisateur = "ADMIN" | "CLIENT" | "SUPPORT";

/** Utilisateur tel que renvoyé par /api/admin/users (SCRUM-97 / SCRUM-108). */
export interface UtilisateurAdmin {
  id: number;
  email: string;
  nom: string | null;
  role: RoleUtilisateur;
  actif: boolean;
  /** Null pour les comptes créés avant l'ajout de la colonne. */
  dateCreation: string | null;
}

/** Page Spring Data renvoyée par le backend. */
export interface PageUtilisateurs {
  content: UtilisateurAdmin[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltresUtilisateurs {
  role?: RoleUtilisateur;
  actif?: boolean;
  page?: number;
  size?: number;
  sort?: "id" | "email" | "nom" | "role" | "actif" | "dateCreation";
  direction?: "asc" | "desc";
}

/** Liste paginée et filtrable des comptes (ADMIN). */
export function listerUtilisateurs(filtres: FiltresUtilisateurs = {}): Promise<PageUtilisateurs> {
  const parametres = new URLSearchParams();
  if (filtres.role) parametres.set("role", filtres.role);
  if (filtres.actif !== undefined) parametres.set("actif", String(filtres.actif));
  parametres.set("page", String(filtres.page ?? 0));
  parametres.set("size", String(filtres.size ?? 20));
  parametres.set("sort", filtres.sort ?? "id");
  parametres.set("direction", filtres.direction ?? "asc");

  return appelApi<PageUtilisateurs>(`/admin/users?${parametres.toString()}`);
}

/** Active ou suspend un compte ; la suspension révoque aussi ses refresh tokens. */
export function changerStatutUtilisateur(
  id: number,
  actif: boolean,
  motif?: string
): Promise<UtilisateurAdmin> {
  return appelApi<UtilisateurAdmin>(`/admin/users/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ actif, motif }),
  });
}

/** Change le rôle d'un utilisateur (un admin ne peut pas changer le sien). */
export function changerRoleUtilisateur(id: number, role: RoleUtilisateur): Promise<UtilisateurAdmin> {
  return appelApi<UtilisateurAdmin>(`/admin/users/${id}/role`, {
    method: "PATCH",
    body: JSON.stringify({ role }),
  });
}

export const messageErreur = messageErreurApi;
