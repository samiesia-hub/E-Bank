import { describe, expect, it } from "vitest";
import { PERIODES, PERIODE_PAR_DEFAUT, lirePeriode } from "./periodes";

describe("PERIODES", () => {
  it("propose trois périodes avec leur libellé", () => {
    expect(PERIODES.map((p) => p.valeur)).toEqual(["aujourdhui", "7j", "30j"]);
    expect(PERIODES.every((p) => p.label.length > 0)).toBe(true);
  });

  it("a pour période par défaut 30 jours", () => {
    expect(PERIODE_PAR_DEFAUT).toBe("30j");
  });
});

describe("lirePeriode", () => {
  it.each(["aujourdhui", "7j", "30j"])("accepte la période %j", (valeur) => {
    expect(lirePeriode(valeur)).toBe(valeur);
  });

  it("revient à la période par défaut quand la valeur est inconnue", () => {
    expect(lirePeriode("abc")).toBe(PERIODE_PAR_DEFAUT);
    expect(lirePeriode("")).toBe(PERIODE_PAR_DEFAUT);
  });

  it("revient à la période par défaut quand le paramètre est absent", () => {
    expect(lirePeriode(undefined)).toBe(PERIODE_PAR_DEFAUT);
  });

  it("lit la première valeur quand le paramètre est répété", () => {
    expect(lirePeriode(["7j", "30j"])).toBe("7j");
    expect(lirePeriode(["abc", "7j"])).toBe(PERIODE_PAR_DEFAUT);
  });

  it("gère un tableau vide", () => {
    expect(lirePeriode([])).toBe(PERIODE_PAR_DEFAUT);
  });
});
