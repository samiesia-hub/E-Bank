"use client";

import Image from "next/image";
import { AlertCircle, BadgeCheck, Ticket } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { Bouton } from "@/components/interface/Bouton";
import { useDashboardAdmin } from "@/features/admin/useDashboardAdmin";
import { ErreurApi } from "@/lib/client-api";
import { formaterNombre } from "@/lib/formatage";

/**
 * Vérifie si l'erreur correspond à une absence d'autorisation (401 ou 403).
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

interface PropsCarteIndicateur {
  libelle: string;
  valeur: string;
  illustration: React.ReactNode;
}

/**
 * Carte simple réutilisant la structure de CarteKpi sans tendance.
 * Choix : un sous-composant local dédié a été préféré afin de garder le composant autonome,
 * simple et capable d'accueillir des cercles d'icônes Lucide sans complexifier CarteKpi.
 */
function CarteIndicateur({ libelle, valeur, illustration }: PropsCarteIndicateur) {
  return (
    <Carte className="flex flex-col justify-between" style={{ gap: "16px" }}>
      <div className="flex items-start justify-between" style={{ gap: "12px" }}>
        <p className="text-label-md text-neutral-core">{libelle}</p>
        {illustration}
      </div>
      <p className="text-headline-md font-semibold tabular-nums text-navy">
        {valeur}
      </p>
    </Carte>
  );
}

/**
 * Affiche les 3 indicateurs clés d'administration : utilisateurs actifs, tickets ouverts,
 * et dossiers KYC en attente, avec gestion des états chargement, succès et erreur.
 */
export function IndicateursCards() {
  const { data, isLoading, isError, error, refetch } = useDashboardAdmin();

  if (isError) {
    const message = estErreurNonAutorisee(error)
      ? "Accès réservé aux administrateurs."
      : "Impossible de charger les indicateurs. Vérifiez votre connexion puis réessayez.";

    return (
      <section aria-label="Indicateurs clés">
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between rounded-lg border border-danger/30 bg-danger/10 p-space-md text-body-sm text-danger"
          style={{ gap: "16px" }}
        >
          <div className="flex items-center" style={{ gap: "12px" }}>
            <AlertCircle size={20} aria-hidden="true" className="shrink-0" />
            <span>{message}</span>
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
      </section>
    );
  }

  const valeurUtilisateurs =
    isLoading || !data ? "—" : formaterNombre(data.utilisateursActifs);
  const valeurTickets =
    isLoading || !data ? "—" : formaterNombre(data.ticketsOuverts);
  const valeurKyc =
    isLoading || !data ? "—" : formaterNombre(data.dossiersKycEnAttente);

  return (
    <section
      aria-label="Indicateurs clés"
      className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
      style={{ gap: "24px" }}
    >
      <CarteIndicateur
        libelle="Utilisateurs actifs"
        valeur={valeurUtilisateurs}
        illustration={
          <Image
            src="/clients.png"
            alt=""
            width={96}
            height={96}
            className="shrink-0 object-contain"
            style={{ width: "40px", height: "40px" }}
          />
        }
      />
      <CarteIndicateur
        libelle="Tickets ouverts"
        valeur={valeurTickets}
        illustration={
          <div
            className="flex shrink-0 items-center justify-center rounded-full bg-primary-container/10 text-primary-container"
            style={{ width: "48px", height: "48px" }}
          >
            <Ticket size={24} aria-hidden="true" />
          </div>
        }
      />
      <CarteIndicateur
        libelle="Dossiers KYC en attente"
        valeur={valeurKyc}
        illustration={
          <div
            className="flex shrink-0 items-center justify-center rounded-full bg-ora-gold/20 text-navy"
            style={{ width: "48px", height: "48px" }}
          >
            <BadgeCheck size={24} aria-hidden="true" />
          </div>
        }
      />
    </section>
  );
}

