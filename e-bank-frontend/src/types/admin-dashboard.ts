/**
 * Types pour le tableau de bord administrateur (SCRUM-42 / SCRUM-43).
 * Contrat d'interface avec le backend : ne pas modifier les noms de champs.
 */

export interface EntreeAudit {
  id: number;
  action: string;
  auteur: string;
  date: string; // Date au format ISO envoyé par le backend
}

export interface DashboardAdmin {
  utilisateursActifs: number;
  ticketsOuverts: number;
  dossiersKycEnAttente: number;
  activitesRecentes: EntreeAudit[];
}

