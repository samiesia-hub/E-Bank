import { describe, expect, it } from "vitest";
import {
  estIbanPlausible,
  formatDate,
  formatIban,
  formatMoney,
  formaterDate,
  formaterDateHeure,
  formaterDateLongue,
  formaterIban,
  formaterMontant,
  formaterMontantCompact,
  formaterNombre,
  formaterTendance,
  isPlausibleIban,
  maskIban,
  masquerIban,
} from "./formatage";

const IBAN = "FR7630004028379876543210985";
const POINTS = "\u2022".repeat(4);

/** Intl insère des espaces insécables : on les remplace par des espaces normaux. */
const normaliser = (texte: string) => texte.replace(/[\u00a0\u202f]/g, " ");

describe("formaterMontant", () => {
  it("formate un montant en français avec sa devise", () => {
    const texte = normaliser(formaterMontant({ montant: "1500.5", devise: "MAD" }));

    expect(texte).toContain("1 500,50");
    expect(texte).toContain("MAD");
  });

  it("respecte la locale demandée", () => {
    const texte = formaterMontant({ montant: "1500", devise: "EUR" }, "en-US");

    expect(texte).toContain("1,500.00");
  });

  it("renvoie le texte brut quand le montant n'est pas un nombre", () => {
    expect(formaterMontant({ montant: "abc", devise: "MAD" })).toBe("abc MAD");
  });
});

describe("formaterIban", () => {
  it("regroupe par blocs de quatre en majuscules", () => {
    expect(formaterIban("fr76 3000 4028 3798 7654 3210 985")).toBe(
      "FR76 3000 4028 3798 7654 3210 985",
    );
  });

  it("accepte un IBAN sans espaces", () => {
    expect(formaterIban(IBAN)).toBe("FR76 3000 4028 3798 7654 3210 985");
  });
});

describe("estIbanPlausible", () => {
  it("accepte un IBAN bien formé, avec ou sans espaces et en minuscules", () => {
    expect(estIbanPlausible(IBAN)).toBe(true);
    expect(estIbanPlausible("fr76 3000 4028 3798 7654 3210 985")).toBe(true);
  });

  it("refuse les formes invalides", () => {
    expect(estIbanPlausible("1234")).toBe(false);
    expect(estIbanPlausible("FR76")).toBe(false);
    expect(estIbanPlausible("FRAB30004028379876543210985")).toBe(false);
    expect(estIbanPlausible("")).toBe(false);
  });
});

describe("masquerIban", () => {
  it("masque le milieu et garde le début et la fin", () => {
    expect(masquerIban(IBAN)).toBe(`FR76 ${POINTS} ${POINTS} ${POINTS} 985`);
  });

  it("masque entièrement un IBAN trop court", () => {
    expect(masquerIban("FR76")).toBe(POINTS);
  });
});

describe("formaterDate", () => {
  it("formate une date ISO au format jj/mm/aaaa", () => {
    expect(formaterDate("2026-11-25T12:00:00Z")).toBe("25/11/2026");
  });

  it("renvoie la valeur d'origine quand la date est invalide", () => {
    expect(formaterDate("pas une date")).toBe("pas une date");
  });
});

describe("formaterDateLongue", () => {
  it("écrit la date en toutes lettres", () => {
    const texte = formaterDateLongue(new Date("2026-11-25T12:00:00Z"));

    expect(texte).toBe("mercredi 25 novembre 2026");
  });

  it("utilise la date du jour par défaut", () => {
    expect(formaterDateLongue()).toMatch(/\d{4}$/);
  });
});

describe("formaterNombre", () => {
  it("sépare les milliers", () => {
    expect(normaliser(formaterNombre(12480))).toBe("12 480");
  });
});

describe("formaterMontantCompact", () => {
  it("exprime les millions avec une décimale", () => {
    expect(formaterMontantCompact({ montant: "186400000", devise: "MAD" })).toBe(
      "186,4 M MAD",
    );
  });

  it("garde l'entier sous le million", () => {
    expect(
      normaliser(formaterMontantCompact({ montant: "12480", devise: "MAD" })),
    ).toBe("12 480 MAD");
  });

  it("traite aussi les grands montants négatifs", () => {
    expect(
      normaliser(formaterMontantCompact({ montant: "-2500000", devise: "MAD" })),
    ).toContain("2,5 M MAD");
  });

  it("renvoie le texte brut quand le montant n'est pas un nombre", () => {
    expect(formaterMontantCompact({ montant: "x", devise: "MAD" })).toBe("x MAD");
  });
});

describe("formaterTendance", () => {
  it("ajoute le signe plus aux hausses", () => {
    expect(formaterTendance(3.2)).toBe("+3,2 %");
  });

  it("affiche les baisses avec un signe moins", () => {
    expect(formaterTendance(-1.5)).toMatch(/^[-\u2212]1,5 %$/);
  });

  it("n'affiche pas de signe pour zéro", () => {
    expect(formaterTendance(0)).toBe("0 %");
  });
});

describe("formaterDateHeure", () => {
  it("affiche la date et l'heure", () => {
    expect(formaterDateHeure("2026-10-31T14:30:00")).toMatch(/31\/10\/2026.*14:30/);
  });

  it("renvoie la valeur d'origine quand la date est invalide", () => {
    expect(formaterDateHeure("invalide")).toBe("invalide");
  });
});

describe("alias anglais", () => {
  it("pointent vers les mêmes fonctions", () => {
    expect(formatMoney).toBe(formaterMontant);
    expect(formatIban).toBe(formaterIban);
    expect(isPlausibleIban).toBe(estIbanPlausible);
    expect(maskIban).toBe(masquerIban);
    expect(formatDate).toBe(formaterDate);
  });
});
