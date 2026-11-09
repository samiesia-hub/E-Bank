"use client";

import { use } from "react";
import Link from "next/link";
import { ArrowLeft, ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteTransaction } from "@/components/interface/Squelettes";
import { useTransactionsDuCompte } from "@/features/client/hooks";
import { formaterDateHeure } from "@/lib/formatage";

interface Props {
  params: Promise<{ id: string }>;
}

export default function PageDetailCompte({ params }: Props) {
  const { id } = use(params);
  const compteId = Number(id);

  const { data, isLoading, isError, error, refetch } = useTransactionsDuCompte(compteId, {
    size: 20,
  });

  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <Link
        href="/comptes"
        className="inline-flex items-center gap-space-xs font-label-sm text-label-sm text-primary hover:underline"
      >
        <ArrowLeft size={14} />
        Retour à mes comptes
      </Link>

      <p className="mt-space-lg font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Compte {id}
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Historique des transactions
      </h1>

      <div className="mt-space-xl">
        {isError && <BanniereErreur erreur={error} reessayer={refetch} />}

        <Carte className="p-0">
          {isLoading && (
            <ul>
              {Array.from({ length: 5 }).map((_, i) => (
                <SqueletteTransaction key={i} />
              ))}
            </ul>
          )}

          {!isLoading && !isError && data?.contenu.length === 0 && (
            <div className="flex min-h-52 flex-col items-center justify-center p-space-lg text-center">
              <p className="font-label-md text-label-md font-semibold text-navy">
                Aucune transaction
              </p>
              <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                Les transactions de ce compte apparaîtront ici.
              </p>
            </div>
          )}

          {!isLoading && !isError && data && data.contenu.length > 0 && (
            <ul className="divide-y divide-divider">
              {data.contenu.map((tx) => {
                const estCredit = tx.sens === "CREDIT";
                const Icone = estCredit ? ArrowDownLeft : ArrowUpRight;
                return (
                  <li
                    key={tx.id}
                    className="flex items-center gap-space-md px-space-lg py-space-md"
                  >
                    <span
                      className={`grid h-10 w-10 shrink-0 place-items-center rounded-full ${
                        estCredit
                          ? "bg-[#e8f8f0] text-[#238b5c]"
                          : "bg-[#fff1ed] text-[#d85d3f]"
                      }`}
                    >
                      <Icone size={18} aria-hidden="true" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-label-md text-label-md font-semibold text-on-surface">
                        {tx.libelle || (estCredit ? "Crédit" : "Débit")}
                      </p>
                      <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                        {formaterDateHeure(tx.dateTransaction)}
                      </p>
                    </div>
                    <p
                      className={`shrink-0 font-label-md text-label-md font-semibold tabular ${
                        estCredit ? "text-[#238b5c]" : "text-on-surface"
                      }`}
                    >
                      {estCredit ? "+ " : "− "}
                      {new Intl.NumberFormat("fr-FR", {
                        style: "currency",
                        currency: "MAD",
                      }).format(Math.abs(tx.montant))}
                    </p>
                  </li>
                );
              })}
            </ul>
          )}
        </Carte>

        {/* Pagination simple */}
        {!isLoading && !isError && data && data.totalPages > 1 && (
          <p className="mt-space-md text-center font-body-sm text-body-sm text-neutral-core">
            Page 1 / {data.totalPages} —{" "}
            <span className="text-primary">
              pagination complète à venir
            </span>
          </p>
        )}
      </div>
    </main>
  );
}
