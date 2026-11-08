"use client";

import Link from "next/link";
import { Landmark } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteCarteCompte } from "@/components/interface/Squelettes";
import { useComptes } from "@/features/client/hooks";

const libellesTypes: Record<string, string> = {
  COURANT: "Compte courant",
  EPARGNE: "Compte épargne",
  PROFESSIONNEL: "Compte professionnel",
};

const couleursStatut: Record<string, string> = {
  ACTIF: "bg-[#e8f8f0] text-[#238b5c]",
  BLOQUE: "bg-[#fff1ed] text-[#d85d3f]",
  CLOTURE: "bg-surface-container text-on-surface-variant",
};

export default function PageMesComptes() {
  const { data: comptes, isLoading, isError, error, refetch } = useComptes();

  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Ora Bank — Mes comptes
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Mes comptes
      </h1>
      <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
        Consultez vos comptes courant, épargne et professionnel.
      </p>

      <div className="mt-space-xl">
        {/* Erreur réseau */}
        {isError && (
          <BanniereErreur erreur={error} reessayer={refetch} />
        )}

        {/* Skeletons */}
        {isLoading && (
          <div className="grid gap-gutter md:grid-cols-2 xl:grid-cols-3">
            <SqueletteCarteCompte />
            <SqueletteCarteCompte />
            <SqueletteCarteCompte />
          </div>
        )}

        {/* Aucun compte */}
        {!isLoading && !isError && comptes && comptes.length === 0 && (
          <Carte className="flex min-h-56 flex-col items-center justify-center text-center">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-surface-container text-primary">
              <Landmark size={23} />
            </span>
            <p className="mt-space-md font-label-md text-label-md font-semibold text-navy">
              Aucun compte bancaire
            </p>
            <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
              Contactez votre conseiller pour ouvrir un compte.
            </p>
          </Carte>
        )}

        {/* Liste des comptes */}
        {!isLoading && !isError && comptes && comptes.length > 0 && (
          <div className="grid gap-gutter md:grid-cols-2 xl:grid-cols-3">
            {comptes.map((compte) => (
              <Link key={compte.id} href={`/comptes/${compte.id}`} className="group block">
                <Carte className="relative h-full overflow-hidden transition-shadow group-hover:shadow-md">
                  <div className="absolute right-0 top-0 h-20 w-20 rounded-bl-full bg-primary-fixed/40" />
                  <div className="relative flex items-start justify-between gap-space-md">
                    <span className="grid h-10 w-10 place-items-center rounded-lg bg-surface-container text-primary">
                      <Landmark size={20} aria-hidden="true" />
                    </span>
                    <span
                      className={`rounded-full px-2 py-1 font-label-sm text-label-sm ${
                        couleursStatut[compte.statutCompte] ?? "bg-surface-container text-on-surface-variant"
                      }`}
                    >
                      {compte.statutCompte.toLowerCase()}
                    </span>
                  </div>
                  <p className="mt-space-lg font-label-md text-label-md text-on-surface-variant">
                    {libellesTypes[compte.typeCompte] ?? compte.typeCompte}
                  </p>
                  <p className="mt-space-xs font-numeric-data text-numeric-data font-semibold text-navy tabular">
                    {new Intl.NumberFormat("fr-FR", {
                      style: "currency",
                      currency: compte.devise ?? "MAD",
                    }).format(compte.solde)}
                  </p>
                  <p className="mt-space-md font-body-sm text-body-sm text-neutral-core">
                    {compte.rib
                      ? `RIB : ${compte.rib.slice(0, 4)} •••• ${compte.rib.slice(-4)}`
                      : compte.numeroCompte}
                  </p>
                </Carte>
              </Link>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
