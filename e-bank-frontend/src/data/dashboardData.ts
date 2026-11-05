
export type Account = {
  id: number;
  name: string;
  balance: number;
  rib: string;
};

export type Transaction = {
  id: number;
  date: string;
  label: string;
  category: string;
  amount: number;
  status: "Validé" | "En attente";
};

export type BankNotification = {
  id: number;
  title: string;
  message: string;
  date: string;
  unread: boolean;
};

// Informations du client
export const client = {
  firstName: "Client",
  lastName: "",
  initials: "",
  lastLogin: "",
};

// Comptes bancaires
export const accounts: Account[] = [];

// Dernières transactions
export const transactions: Transaction[] = [];

// Dépenses mensuelles : deux séries de comparaison
export const monthlyExpenses: {
  month: string;
  current: number;
  previous: number;
}[] = [];

// Crédit en cours
export const loan = {
  name: "Aucun crédit en cours",
  totalAmount: 0,
  monthlyPayment: 0,
  paidInstallments: 0,
  totalInstallments: 0,
  nextPayment: "Non disponible",
};

// Notifications
export const notifications: BankNotification[] = [];

// Format monétaire marocain
export function formatMAD(amount: number): string {
  return (
    new Intl.NumberFormat("fr-FR", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount) + " MAD"
  );
}
