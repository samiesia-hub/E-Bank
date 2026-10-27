import { TrendingDown, TrendingUp, type LucideIcon } from "lucide-react";
import { clsx } from "clsx";
import { Carte } from "@/components/interface/Carte";
import { formaterTendance } from "@/lib/formatage";

interface PropsCarteKpi {
    libelle: string;
    valeur: string | null; // déjà formatée ; null = pas de donnée
    tendance: number | null; // variation en % ; null = pas de donnée
    icone: LucideIcon;
    // false pour les indicateurs où une hausse est mauvaise (ex. alertes fraude)
    hausseEstPositive?: boolean;
}

export function CarteKpi({
                             libelle,
                             valeur,
                             tendance,
                             icone: Icone,
                             hausseEstPositive = true,
                         }: PropsCarteKpi) {
    const enHausse = tendance !== null && tendance > 0;
    const enBaisse = tendance !== null && tendance < 0;
    const favorable =
        (enHausse && hausseEstPositive) || (enBaisse && !hausseEstPositive);

    const couleur =
        tendance === null || tendance === 0
            ? "text-neutral-core"
            : favorable
                ? "text-success"
                : "text-danger";

    const IconeTendance = enBaisse ? TrendingDown : TrendingUp;

    return (
        <Carte className="flex flex-col gap-space-md">
            <div className="flex items-center justify-between gap-space-sm">
                <p className="text-label-md text-neutral-core">{libelle}</p>
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-fixed text-primary-container">
          <Icone size={20} aria-hidden="true" />
        </span>
            </div>

            <p className="text-headline-md font-semibold tabular-nums text-navy">
                {valeur ?? "—"}
            </p>

            <p className={clsx("flex items-center gap-1 text-label-md", couleur)}>
                {tendance === null ? (
                    <span>Aucune donnée</span>
                ) : (
                    <>
                        <IconeTendance size={16} aria-hidden="true" />
                        <span className="tabular-nums">{formaterTendance(tendance)}</span>
                        <span className="text-neutral-core">vs période précédente</span>
                    </>
                )}
            </p>
        </Carte>
    );
}