"use client";

import { AlertCircle } from "lucide-react";
import { Bouton } from "@/components/interface/Bouton";
import { useDashboardAdmin } from "@/features/admin/useDashboardAdmin";
import { ErreurApi } from "@/lib/client-api";
import { formaterDateHeure } from "@/lib/formatage";

/**
 * Vérifie si l'erreur correspond à un problème de droits ou d'authentification (401 ou 403).
 */
function estErreurNonAutorisee(error: unknown): boolean {
  if (error instanceof ErreurApi) {
    return error.status === 401 || error.status === 403;
  }
  if (typeof error === "object" && error !== null && "status" in error) {
    const s = (error as { status: unknown }).status;
    return s === 401 || s === 403;
  }
  return false;
}

/**
 * Affiche la liste des dernières entrées du journal d'audit de la banque.
 * Gère les états de chargement, d'erreur, de liste vide ou d'affichage des activités.
 */
export function ActivitesRecentes() {
  const { data, isLoading, isError, error, refetch } = useDashboardAdmin();

  return (
    <div className="flex flex-col" style={{ gap: "16px" }}>
      <div className="flex items-center justify-between">
        <h2 className="text-headline-sm font-semibold text-navy">
          Activités récentes
        </h2>
      </div>

      {isError ? (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between rounded-lg border border-danger/30 bg-danger/10 p-space-md text-body-sm text-danger"
          style={{ gap: "16px" }}
        >
          <div className="flex items-center" style={{ gap: "12px" }}>
            <AlertCircle size={20} aria-hidden="true" className="shrink-0" />
            <span>
              {estErreurNonAutorisee(error)
                ? "Accès réservé aux administrateurs."
                : "Impossible de charger les activités récentes. Vérifiez votre connexion puis réessayez."}
            </span>
          </div>
          <Bouton
            type="button"
            variant="outline"
            onClick={() => {
              void refetch();
            }}
            className="border-danger text-danger hover:bg-danger/10"
          >
            Réessayer
          </Bouton>
        </div>
      ) : isLoading ? (
        <div className="py-space-lg text-center text-body-sm text-neutral-core">
          Chargement des activités récentes…
        </div>
      ) : !data?.activitesRecentes || data.activitesRecentes.length === 0 ? (
        <p className="py-space-lg text-center text-body-sm text-neutral-core">
          Aucune activité récente
        </p>
      ) : (
        <ul className="divide-y divide-divider" role="list">
          {data.activitesRecentes.map((entree) => (
            <li
              key={entree.id}
              className="flex flex-col sm:flex-row sm:items-center sm:justify-between py-space-sm"
              style={{ gap: "8px" }}
            >
              <div>
                <p className="text-body-md font-medium text-navy">
                  {entree.action}
                </p>
                <p className="text-body-sm text-neutral-core">
                  Par {entree.auteur}
                </p>
              </div>
              <time
                dateTime={entree.date}
                className="text-body-sm tabular-nums text-neutral-core"
              >
                {formaterDateHeure(entree.date)}
              </time>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

