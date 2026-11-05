// Périodes proposées dans le filtre du dashboard admin
export type Periode = "aujourdhui" | "7j" | "30j";

export const PERIODES: { valeur: Periode; label: string }[] = [
    { valeur: "aujourdhui", label: "Aujourd'hui" },
    { valeur: "7j", label: "7 jours" },
    { valeur: "30j", label: "30 jours" },
];

export const PERIODE_PAR_DEFAUT: Periode = "7j";

// L'URL peut contenir n'importe quoi (?periode=abc) : on valide avant d'utiliser.
export function lirePeriode(brut: string | string[] | undefined): Periode {
    const valeur = Array.isArray(brut) ? brut[0] : brut;
    return PERIODES.find((p) => p.valeur === valeur)?.valeur ?? PERIODE_PAR_DEFAUT;
}