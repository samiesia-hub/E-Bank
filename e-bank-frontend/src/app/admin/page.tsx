import { Carte } from "@/components/interface/Carte";
import { ActivitesRecentes } from "@/features/admin/ActivitesRecentes";
import { IndicateursCards } from "@/features/admin/IndicateursCards";
import { formaterDateLongue } from "@/lib/formatage";

/**
 * Page principale du tableau de bord d’administration (vue d’ensemble).
 * Affiche la date du jour, les 3 indicateurs clés de la banque et les activités récentes.
 */
export default function PageVueEnsemble() {
  return (
    <main
      className="px-margin py-space-xl flex flex-col"
      style={{ gap: "32px" }}
    >
      <div>
        <h1 className="text-headline-md font-semibold text-navy">
          Vue d’ensemble
        </h1>
        <p className="mt-space-xs text-body-sm text-neutral-core first-letter:uppercase">
          {formaterDateLongue()}
        </p>
      </div>

      <IndicateursCards />

      <section aria-label="Activités récentes">
        <Carte>
          <ActivitesRecentes />
        </Carte>
      </section>
    </main>
  );
}
