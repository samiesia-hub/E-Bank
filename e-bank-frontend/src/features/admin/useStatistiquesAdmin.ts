import { useQuery } from "@tanstack/react-query";
import { appelApi } from "@/lib/client-api";
import type { Periode } from "./periodes";
import type { StatistiquesAdmin } from "./types";

export function useStatistiquesAdmin(periode: Periode) {
    return useQuery({
        queryKey: ["admin", "statistiques", periode],
        queryFn: () =>
            appelApi<StatistiquesAdmin>(`/admin/statistiques?periode=${periode}`),
    });
}