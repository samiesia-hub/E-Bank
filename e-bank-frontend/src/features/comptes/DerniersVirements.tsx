import { ArrowDownLeft, ArrowUpRight, Send } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { formaterDate, formaterMontant } from "@/lib/formatage";
import type { Virement } from "@/types/banque";

export function DerniersVirements({ virements }: { virements: Virement[] }) {
  return (
    <section aria-labelledby="titre-virements">
      <div className="mb-space-md">
        <h2 id="titre-virements" className="font-headline-sm text-headline-sm font-semibold text-navy">Derniers virements</h2>
        <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">Les cinq opérations les plus récentes</p>
      </div>

      <Carte className="p-0">
        {virements.length === 0 ? (
          <div className="p-space-lg text-center">
            <Send className="mx-auto text-primary" size={24} aria-hidden="true" />
            <p className="mt-space-sm font-body-md text-body-md text-neutral-core">Aucun virement récent.</p>
          </div>
        ) : (
          <ul className="divide-y divide-divider">
            {virements.slice(0, 5).map((virement) => {
              const estEntrant = Number(virement.montant.montant) > 0;
              const Icone = estEntrant ? ArrowDownLeft : ArrowUpRight;
              const libelle = virement.libelle || virement.motif || "Virement";

              return (
                <li key={virement.id} className="flex items-center gap-space-md px-space-lg py-space-md">
                  <span className={`grid h-10 w-10 shrink-0 place-items-center rounded-full ${estEntrant ? "bg-[#e8f8f0] text-[#238b5c]" : "bg-[#fff1ed] text-[#d85d3f]"}`}>
                    <Icone size={18} aria-hidden="true" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-label-md text-label-md font-semibold text-on-surface">{libelle}</p>
                    <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">{formaterDate(virement.dateExecution ?? virement.date)}</p>
                  </div>
                  <p className={`shrink-0 font-label-md text-label-md font-semibold tabular ${estEntrant ? "text-[#238b5c]" : "text-on-surface"}`}>
                    {estEntrant ? "+ " : ""}{formaterMontant(virement.montant)}
                  </p>
                </li>
              );
            })}
          </ul>
        )}
      </Carte>
    </section>
  );
}
