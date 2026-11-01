import { Landmark } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { formaterMontant, masquerIban } from "@/lib/formatage";
import type { CompteBancaire } from "@/types/banque";

const libellesTypes: Record<CompteBancaire["type"], string> = {
  COURANT: "Compte courant",
  EPARGNE: "Compte épargne",
  PROFESSIONNEL: "Compte professionnel",
};

export function CartesComptes({ comptes }: { comptes: CompteBancaire[] }) {
  return (
    <section aria-labelledby="titre-comptes">
      <div className="mb-space-md flex items-end justify-between gap-space-md">
        <div>
          <h2 id="titre-comptes" className="font-headline-sm text-headline-sm font-semibold text-navy">Mes comptes</h2>
          <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">Vos soldes disponibles</p>
        </div>
      </div>

      {comptes.length === 0 ? (
        <Carte>
          <p className="font-body-md text-body-md text-neutral-core">Aucun compte bancaire n&apos;est associé à votre espace.</p>
        </Carte>
      ) : (
        <div className="grid gap-gutter md:grid-cols-2 xl:grid-cols-3">
          {comptes.map((compte) => (
            <Carte key={compte.id} className="relative overflow-hidden">
              <div className="absolute right-0 top-0 h-20 w-20 rounded-bl-full bg-primary-fixed/40" />
              <div className="relative flex items-start justify-between gap-space-md">
                <span className="grid h-10 w-10 place-items-center rounded-lg bg-surface-container text-primary">
                  <Landmark size={20} aria-hidden="true" />
                </span>
                <span className="rounded-full bg-surface-container px-2 py-1 font-label-sm text-label-sm text-on-surface-variant">
                  {compte.statut.toLowerCase()}
                </span>
              </div>
              <p className="mt-space-lg font-label-md text-label-md text-on-surface-variant">{libellesTypes[compte.type]}</p>
              <p className="mt-space-xs font-numeric-data text-numeric-data font-semibold text-navy tabular">{formaterMontant(compte.solde)}</p>
              <p className="mt-space-md font-body-sm text-body-sm text-neutral-core">{masquerIban(compte.iban)}</p>
            </Carte>
          ))}
        </div>
      )}
    </section>
  );
}
