import type { Montant } from "@/types/banque";

// Contrat attendu du backend : GET /admin/statistiques?periode=7j
// (proposition à valider avec l'équipe : l'endpoint n'existe pas encore)
export interface StatistiquesAdmin {
    totalClients: number;
    totalDepots: Montant;
    transactionsDuJour: number;
    alertesFraude: number;
    // Variation en % par rapport à la période précédente (ex. 3.2 ou -1.5)
    tendances: {
        totalClients: number;
        totalDepots: number;
        transactionsDuJour: number;
        alertesFraude: number;
    };
}