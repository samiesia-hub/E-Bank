import { FiltrePeriode } from "@/components/admin/FiltrePeriode";
import { lirePeriode } from "@/features/admin/periodes";
import { formaterDateLongue } from "@/lib/formatage";
import { GrilleKpi } from "@/components/admin/GrilleKpi";
export default async function PageVueEnsemble({
                                                  searchParams,
                                              }: {
    searchParams: Promise<{ periode?: string | string[] }>;
}) {
    const { periode: periodeBrute } = await searchParams;
    const periode = lirePeriode(periodeBrute);

    return (
        <main className="px-margin py-space-xl">
            <div className="flex flex-wrap items-end justify-between gap-space-md">
                <div>
                    <h1 className="text-headline-md font-semibold text-navy">
                        Vue d'ensemble
                    </h1>
                    <p className="mt-space-xs text-body-sm text-neutral-core first-letter:uppercase">
                        {formaterDateLongue()}
                    </p>
                </div>
                <FiltrePeriode active={periode} />
            </div>

            <GrilleKpi periode={periode} />
        </main>
    );
}