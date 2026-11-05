import { FiltrePeriode } from "@/components/admin/FiltrePeriode";
import { lirePeriode } from "@/features/admin/periodes";
import { formaterDateLongue } from "@/lib/formatage";
import { GrilleKpi } from "@/components/admin/GrilleKpi";
import { Carte } from "@/components/interface/Carte";
import { ActivitesRecentes } from "@/features/admin/ActivitesRecentes";

/**
 * Page principale du tableau de bord d’administration (vue d’ensemble).
 * Affiche la date du jour, les indicateurs clés filtrables par période
 * et les activités récentes de la banque.
 */
export default async function PageVueEnsemble({
    searchParams,
}: {
    searchParams: Promise<{ periode?: string | string[] }>;
}) {
    const { periode: periodeBrute } = await searchParams;
    const periode = lirePeriode(periodeBrute);

    return (
        <main className="px-margin py-space-xl flex flex-col gap-space-xl">
            <div className="flex flex-wrap items-end justify-between gap-space-md">
                <div>
                    <h1 className="text-headline-md font-semibold text-navy">
                        Vue d&apos;ensemble
                    </h1>
                    <p className="mt-space-xs text-body-sm text-neutral-core first-letter:uppercase">
                        {formaterDateLongue()}
                    </p>
                </div>
                <FiltrePeriode active={periode} />
            </div>

            <GrilleKpi periode={periode} />

            <section aria-label="Activités récentes">
                <Carte>
                    <ActivitesRecentes />
                </Carte>
            </section>
        </main>
    );
}
