import { describe, expect, it } from "vitest";
import { CLE_EMAIL_REINIT, REGLE_MOT_DE_PASSE, motDePasseValide } from "./reinitialisation";

describe("constantes du parcours mot de passe oublié", () => {
  it("expose la clé de stockage de l'e-mail", () => {
    expect(CLE_EMAIL_REINIT).toBe("bankati.reinit.email");
  });

  it("décrit la règle de mot de passe", () => {
    expect(REGLE_MOT_DE_PASSE).toContain("12");
  });
});

describe("motDePasseValide", () => {
  it("accepte un mot de passe conforme", () => {
    expect(motDePasseValide("Motdepasse123")).toBe(true);
  });

  it("accepte exactement 12 caractères", () => {
    expect(motDePasseValide("Abcdefghij12")).toBe(true);
  });

  it("refuse moins de 12 caractères", () => {
    expect(motDePasseValide("Abcdefghi12")).toBe(false);
  });

  it("refuse l'absence de majuscule", () => {
    expect(motDePasseValide("motdepasse123")).toBe(false);
  });

  it("refuse l'absence de minuscule", () => {
    expect(motDePasseValide("MOTDEPASSE123")).toBe(false);
  });

  it("refuse l'absence de chiffre", () => {
    expect(motDePasseValide("Motdepassesans")).toBe(false);
  });

  it("refuse une chaîne vide", () => {
    expect(motDePasseValide("")).toBe(false);
  });

  it("accepte 72 octets et refuse 73", () => {
    expect(motDePasseValide("Aa1" + "a".repeat(69))).toBe(true);
    expect(motDePasseValide("Aa1" + "a".repeat(70))).toBe(false);
  });

  it("compte les octets et non les caractères", () => {
    // 43 caractères, mais 83 octets en UTF-8 : au-dessus de la limite de 72 octets.
    expect(motDePasseValide("é".repeat(40) + "Aa1")).toBe(false);
  });
});
