// Types du domaine Bankati — miroir du diagramme de classes
// Montant.montant reste une string (jamais de float). Affichage via Intl.NumberFormat.
export type Role = "CLIENT" | "AGENT_SUPPORT" | "ADMIN";

export type StatutCompte = "ACTIF" | "BLOQUE" | "CLOTURE";
export type TypeCompte = "COURANT" | "EPARGNE" | "PROFESSIONNEL";
export type StatutTransaction = "EN_ATTENTE" | "EXECUTEE" | "REJETEE";
export type StatutTicket = "OUVERT" | "EN_TRAITEMENT" | "EN_ATTENTE_CLIENT" | "RESOLU" | "FERME";
export type PrioriteTicket = "BASSE" | "MOYENNE" | "HAUTE" | "CRITIQUE";
export type StatutReclamation = "OUVERTE" | "EN_COURS" | "RESOLUE" | "FERMEE";
export type StatutKYC = "EN_ATTENTE" | "VALIDE" | "REJETE";

export interface Montant {
  montant: string; // ex. "1250.50" — jamais de float
  devise: string; // ex. "EUR" | "MAD"
}

export interface Utilisateur {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  telephone?: string;
  actif: boolean;
  doubleAuthActivee: boolean;
  role: Role;
}

export interface Client extends Utilisateur {
  role: "CLIENT";
  numeroClient: string;
  adresse?: string;
  dateNaissance?: string;
}

export interface CompteBancaire {
  id: number;
  numeroCompte: string;
  iban: string;
  type: TypeCompte;
  solde: Montant;
  devise: string;
  dateOuverture: string;
  statut: StatutCompte;
}

export interface Transaction {
  id: number;
  montant: Montant;
  date: string;
  libelle: string;
  statut: StatutTransaction;
}

export interface Virement extends Transaction {
  motif?: string;
  dateExecution?: string;
  beneficiaireId: number;
}

export interface Beneficiaire {
  id: number;
  nom: string;
  iban: string;
  banque?: string;
  dateAjout: string;
}

export interface Releve {
  periode: string;
  dateGeneration: string;
  format: "PDF" | "CSV";
}

export interface Reclamation {
  id: number;
  objet: string;
  description: string;
  dateCreation: string;
  statut: StatutReclamation;
}

export interface TicketSupport {
  id: number;
  priorite: PrioriteTicket;
  statut: StatutTicket;
  dateOuverture: string;
  dateFermeture?: string;
  clientId: number;
  assigneA?: string;
}

export interface DossierKYC {
  id: number;
  documents: string[];
  dateSoumission: string;
  statut: StatutKYC;
  commentaire?: string;
}

export interface Session {
  utilisateur: Utilisateur;
  expireLe: string;
}

// Alias anglais pour compatibilité
export type { Montant as Money };
