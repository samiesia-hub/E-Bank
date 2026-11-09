"use client";

import Link from "next/link";
import {
  ArrowRight,
  FileWarning,
  Send,
} from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteCarteCompte, SqueletteTransaction } from "@/components/interface/Squelettes";
import { useDashboardClient } from "@/features/client/hooks";
import { formaterDate } from "@/lib/formatage";

const libellesTypes: Record<string, string> = {
  COURANT: "Compte courant",
  EPARGNE: "Compte épargne",
  PROFESSIONNEL: "Compte professionnel",
};

export default function PageTableauDeBord() {
  const { data, isLoading, isError, error, refetch } = useDashboardClient();

  return (
    <main className="mx-auto max-w-[1200px] px-margin pb-24 pt-space-xl">
      {/* En-tête ---------------------------------------------------------------- */}
      <div className="flex flex-wrap items-end justify-between gap-space-md">
        <div>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
            Espace client
          </p>
          <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
            Tableau de bord
          </h1>
          <p className="mt-space-sm font-body-md text-body-md text-neutral-core">
            Consultez vos comptes, vos transactions et vos réclamations.
          </p>
        </div>

        <Link
          href="/virements/nouveau"
          className="inline-flex items-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
        >
          <Send size={17} />
          Faire un virement
        </Link>
      </div>

      {/* Bannière d'erreur globale ---------------------------------------------- */}
      {isError && (
        <div className="mt-space-lg">
          <BanniereErreur erreur={error} reessayer={refetch} />
        </div>
      )}

      {/* Grille principale ------------------------------------------------------ */}
      <div className="mt-space-xl grid gap-gutter lg:grid-cols-[minmax(0,1fr)_300px]">

        {/* Mes comptes ---------------------------------------------------------- */}
        <section>
          <div className="mb-space-md">
            <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
              Mes comptes
            </h2>
            <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
              Vos comptes et soldes disponibles.
            </p>
          </div>

          {isLoading && (
            <div className="grid gap-gutter md:grid-cols-2">
              <SqueletteCarteCompte />
              <SqueletteCarteCompte />
            </div>
          )}

          {!isLoading && !isError && data && data.comptes.length === 0 && (
            <Carte className="flex min-h-48 flex-col items-center justify-center text-center">
              <p className="font-label-md text-label-md font-semibold text-navy">
                Aucun compte associé
              </p>
              <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
                Contactez votre conseiller pour ouvrir un compte.
              </p>
            </Carte>
          )}

          {!isLoading && !isError && data && data.comptes.length > 0 && (
            <div className="grid gap-gutter md:grid-cols-2">
              {data.comptes.map((compte) => (
                <Link key={compte.id} href={`/comptes/${compte.id}`}>
                  <Carte className="relative overflow-hidden transition-shadow hover:shadow-md">
                    <div className="absolute right-0 top-0 h-20 w-20 rounded-bl-full bg-primary-fixed/40" />
                    <div className="relative">
                      <span className="inline-block rounded-full bg-surface-container px-2 py-1 font-label-sm text-label-sm text-on-surface-variant">
                        {compte.statutCompte.toLowerCase()}
                      </span>
                      <p className="mt-space-lg font-label-md text-label-md text-on-surface-variant">
                        {libellesTypes[compte.typeCompte] ?? compte.typeCompte}
                      </p>
                      <p className="mt-space-xs font-numeric-data text-numeric-data font-semibold text-navy tabular">
                        {new Intl.NumberFormat("fr-FR", {
                          style: "currency",
                          currency: "MAD",
                        }).format(compte.solde)}
                      </p>
                      <p className="mt-space-sm font-body-sm text-body-sm text-neutral-core">
                        {compte.numeroCompte}
                      </p>
                    </div>
                  </Carte>
                </Link>
              ))}
            </div>
          )}
        </section>

        {/* Réclamations en cours ------------------------------------------------ */}
        <section
          className="rounded-lg border border-[#f1d99c] bg-[#fffaf0] p-space-lg"
          aria-labelledby="titre-reclamations"
        >
          <FileWarning className="text-[#9c6b06]" size={24} aria-hidden="true" />

          <h2
            id="titre-reclamations"
            className="mt-space-md font-headline-sm text-headline-sm font-semibold text-[#67470a]"
          >
            Réclamations en cours
          </h2>

          {isLoading ? (
            <div className="mt-space-md h-9 w-12 animate-pulse rounded bg-[#f1d99c]" aria-hidden="true" />
          ) : (
            <p className="mt-space-md text-3xl font-semibold text-[#67470a]">
              {isError ? "—" : (data?.nombreReclamationsOuvertes ?? "—")}
            </p>
          )}

          <p className="mt-space-sm font-body-sm text-body-sm text-[#806126]">
            Réclamation(s) ouverte(s) en attente de traitement.
          </p>

          <Link
            href="/reclamations"
            className="mt-space-lg inline-block font-label-md text-label-md text-[#8d6109] hover:underline"
          >
            Voir mes réclamations
          </Link>
        </section>
      </div>

      {/* Derniers virements ----------------------------------------------------- */}
      <section className="mt-space-xl">
        <div className="mb-space-md flex items-end justify-between gap-space-md">
          <div>
            <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
              Dernières transactions
            </h2>
            <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
              Vos cinq opérations les plus récentes.
            </p>
          </div>
          <Link
            href="/comptes"
            className="inline-flex items-center gap-space-xs font-label-md text-label-md text-primary hover:underline"
          >
            Voir tout
            <ArrowRight size={16} />
          </Link>
        </div>

        <Carte className="p-0">
          {isLoading && (
            <ul>
              {[1, 2, 3].map((i) => (
                <SqueletteTransaction key={i} />
              ))}
            </ul>
          )}

          {!isLoading && !isError && data && data.derniersVirements.length === 0 && (
            <div className="flex min-h-52 flex-col items-center justify-center p-space-lg text-center">
              <p className="font-label-md text-label-md font-semibold text-navy">
                Aucune transaction récente
              </p>
              <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
                Vos virements apparaîtront ici après votre première opération.
              </p>
            </div>
          )}

          {!isLoading && !isError && data && data.derniersVirements.length > 0 && (
            <ul className="divide-y divide-divider">
              {data.derniersVirements.slice(0, 5).map((v) => {
                const estCredit = v.montant > 0;
                return (
                  <li
                    key={v.id}
                    className="flex items-center gap-space-md px-space-lg py-space-md"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-label-md text-label-md font-semibold text-on-surface">
                        {v.motif || v.beneficiaire || "Virement"}
                      </p>
                      <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                        {formaterDate(v.dateTransaction)}
                      </p>
                    </div>
                    <p
                      className={`shrink-0 font-label-md text-label-md font-semibold tabular ${
                        estCredit ? "text-[#238b5c]" : "text-on-surface"
                      }`}
                    >
                      {estCredit ? "+ " : ""}
                      {new Intl.NumberFormat("fr-FR", {
                        style: "currency",
                        currency: "MAD",
                      }).format(Math.abs(v.montant))}
                    </p>
                  </li>
                );
              })}
            </ul>
          )}
        </Carte>
      </section>
    </main>
  );
}
