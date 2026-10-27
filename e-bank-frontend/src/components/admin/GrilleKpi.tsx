"use client";

import { ArrowLeftRight, Landmark, ShieldAlert, Users } from "lucide-react";
import { CarteKpi } from "@/components/admin/CarteKpi";
import type { Periode } from "@/features/admin/periodes";
import { useStatistiquesAdmin } from "@/features/admin/useStatistiquesAdmin";
import { formaterMontantCompact, formaterNombre } from "@/lib/formatage";

export function GrilleKpi({ periode }: { periode: Periode }) {
    // data vaut undefined tant que la réponse n'est pas arrivée (ou si l'appel échoue)
    const { data } = useStatistiquesAdmin(periode);

    return (
        <section
            aria-label="Indicateurs clés"
            className="mt-space-lg grid grid-cols-1 gap-space-lg sm:grid-cols-2 xl:grid-cols-4"
        >
            <CarteKpi
                libelle="Total clients"
                icone={Users}
                valeur={data ? formaterNombre(data.totalClients) : null}
                tendance={data?.tendances.totalClients ?? null}
            />
            <CarteKpi
                libelle="Total dépôts"
                icone={Landmark}
                valeur={data ? formaterMontantCompact(data.totalDepots) : null}
                tendance={data?.tendances.totalDepots ?? null}
            />
            <CarteKpi
                libelle="Transactions du jour"
                icone={ArrowLeftRight}
                valeur={data ? formaterNombre(data.transactionsDuJour) : null}
                tendance={data?.tendances.transactionsDuJour ?? null}
            />
            <CarteKpi
                libelle="Alertes fraude"
                icone={ShieldAlert}
                valeur={data ? formaterNombre(data.alertesFraude) : null}
                tendance={data?.tendances.alertesFraude ?? null}
                hausseEstPositive={false}
            />
        </section>
    );
}