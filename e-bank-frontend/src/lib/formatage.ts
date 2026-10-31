import type { Montant } from "@/types/banque";

/** Amounts: Intl fr-FR + tabular figures. Money.amount stays a string. */
export function formaterMontant(montant: Montant, locale = "fr-FR"): string {
  const valeur = Number(montant.montant);
  if (Number.isNaN(valeur)) return `${montant.montant} ${montant.devise}`;
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: montant.devise,
  }).format(valeur);
}

/** "FR76 3000 4028 3798 7654 3210 985" — display helper, not a validator. */
export function formaterIban(iban: string): string {
  return iban.replace(/\s+/g, "").replace(/(.{4})/g, "$1 ").trim().toUpperCase();
}

/** Basic IBAN shape check (backend does the real MOD-97 check). */
export function estIbanPlausible(iban: string): boolean {
  const clean = iban.replace(/\s+/g, "").toUpperCase();
  return /^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(clean);
}

/** Mask for lists: "FR76 •••• •••• •••• 985". */
export function masquerIban(iban: string): string {
  const clean = iban.replace(/\s+/g, "");
  if (clean.length < 8) return "••••";
  return `${clean.slice(0, 4)} •••• •••• •••• ${clean.slice(-3)}`;
}

export function formaterDate(dateIso: string, locale = "fr-FR"): string {
  const d = new Date(dateIso);
  if (Number.isNaN(d.getTime())) return dateIso;
  return new Intl.DateTimeFormat(locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(d);
}

// Alias anglais pour compatibilité
export {
  formaterMontant as formatMoney,
  formaterIban as formatIban,
  estIbanPlausible as isPlausibleIban,
  masquerIban as maskIban,
  formaterDate as formatDate,
};
/** "mardi 27 octobre 2026" — date longue pour les en-têtes de page. */
export function formaterDateLongue(
    date: Date = new Date(),
    locale = "fr-FR"
): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(date);
}
/** "12 480" */
export function formaterNombre(valeur: number, locale = "fr-FR"): string {
  return new Intl.NumberFormat(locale).format(valeur);
}

/** "186,4 M MAD" — montant compact pour les cartes KPI. */
export function formaterMontantCompact(
    montant: Montant,
    locale = "fr-FR"
): string {
  const valeur = Number(montant.montant);
  if (Number.isNaN(valeur)) return `${montant.montant} ${montant.devise}`;

  if (Math.abs(valeur) >= 1_000_000) {
    const millions = new Intl.NumberFormat(locale, {
      maximumFractionDigits: 1,
    }).format(valeur / 1_000_000);
    return `${millions} M ${montant.devise}`;
  }
  const entier = new Intl.NumberFormat(locale, {
    maximumFractionDigits: 0,
  }).format(valeur);
  return `${entier} ${montant.devise}`;
}

/** "+3,2 %" ou "-1,5 %" */
export function formaterTendance(
    pourcentage: number,
    locale = "fr-FR"
): string {
  const texte = new Intl.NumberFormat(locale, {
    maximumFractionDigits: 1,
    signDisplay: "exceptZero",
  }).format(pourcentage);
  return `${texte} %`;
}