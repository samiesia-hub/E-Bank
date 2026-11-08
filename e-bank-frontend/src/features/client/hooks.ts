"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  obtenirDashboard,
  obtenirMesComptes,
  obtenirMesTransactions,
  obtenirTransactionsDuCompte,
  obtenirMonProfil,
  obtenirMesReclamations,
  obtenirReclamation,
  creerReclamation,
  type CreerReclamationPayload,
} from "@/services/api";

/** Tableau de bord complet (comptes + virements + réclamations). */
export function useDashboardClient() {
  return useQuery({
    queryKey: ["client", "dashboard"],
    queryFn: obtenirDashboard,
  });
}

/** Liste des comptes du client connecté. */
export function useComptes() {
  return useQuery({
    queryKey: ["client", "comptes"],
    queryFn: obtenirMesComptes,
  });
}

/** Transactions de tous les comptes du client, 5 premières pour le dashboard. */
export function useTransactionsClient(params?: {
  compteId?: number;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ["client", "transactions", params],
    queryFn: () => obtenirMesTransactions(params),
  });
}

/** Transactions d'un compte précis. */
export function useTransactionsDuCompte(
  compteId: number,
  params?: { page?: number; size?: number }
) {
  return useQuery({
    queryKey: ["client", "comptes", compteId, "transactions", params],
    queryFn: () => obtenirTransactionsDuCompte(compteId, params),
    enabled: compteId > 0,
  });
}

/** Profil du client connecté. */
export function useProfilClient() {
  return useQuery({
    queryKey: ["client", "profil"],
    queryFn: obtenirMonProfil,
  });
}

/** Liste de toutes les réclamations du client connecté. */
export function useReclamations() {
  return useQuery({
    queryKey: ["client", "reclamations"],
    queryFn: obtenirMesReclamations,
  });
}

/** Détail d'une réclamation. */
export function useReclamation(id: number) {
  return useQuery({
    queryKey: ["client", "reclamations", id],
    queryFn: () => obtenirReclamation(id),
    enabled: id > 0,
  });
}

/** Mutation : créer une réclamation, invalide le cache liste. */
export function useCreerReclamation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreerReclamationPayload) => creerReclamation(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["client", "reclamations"] });
      queryClient.invalidateQueries({ queryKey: ["client", "dashboard"] });
    },
  });
}

