import { describe, expect, it } from "vitest";
import type { z } from "zod";
import {
  beneficiarySchema,
  claimSchema,
  loginSchema,
  otpSchema,
  registerSchema,
  schemaBeneficiaire,
  schemaConnexion,
  schemaInscription,
  schemaOtp,
  schemaReclamation,
  schemaVirement,
  transferSchema,
} from "./schemas";

const UUID = "123e4567-e89b-42d3-a456-426614174000";

function estValide(schema: z.ZodType, valeur: unknown): boolean {
  return schema.safeParse(valeur).success;
}

/** Chemins des champs en erreur, ex. ["motDePasse"]. */
function champsEnErreur(schema: z.ZodType, valeur: unknown): string[] {
  const resultat = schema.safeParse(valeur);
  return resultat.success ? [] : resultat.error.issues.map((i) => i.path.join("."));
}

describe("loginSchema", () => {
  it("accepte un e-mail et un mot de passe", () => {
    expect(estValide(loginSchema, { email: "a@b.com", motDePasse: "x" })).toBe(true);
  });

  it("refuse un e-mail invalide", () => {
    expect(champsEnErreur(loginSchema, { email: "pas-un-email", motDePasse: "x" })).toEqual([
      "email",
    ]);
  });

  it("refuse un mot de passe vide", () => {
    expect(champsEnErreur(loginSchema, { email: "a@b.com", motDePasse: "" })).toEqual([
      "motDePasse",
    ]);
  });
});

describe("registerSchema", () => {
  const valide = {
    nom: "Dupont",
    prenom: "Jean",
    email: "jean@example.com",
    telephone: "0600000000",
    motDePasse: "Motdepasse123",
    confirmation: "Motdepasse123",
  };

  it("accepte une inscription complète", () => {
    expect(estValide(registerSchema, valide)).toBe(true);
  });

  it("accepte un téléphone vide ou absent", () => {
    expect(estValide(registerSchema, { ...valide, telephone: "" })).toBe(true);

    const { telephone: _telephone, ...sansTelephone } = valide;
    expect(estValide(registerSchema, sansTelephone)).toBe(true);
  });

  it("refuse un téléphone trop court", () => {
    expect(champsEnErreur(registerSchema, { ...valide, telephone: "123" })).toContain(
      "telephone",
    );
  });

  it("refuse un nom ou un prénom trop courts", () => {
    expect(champsEnErreur(registerSchema, { ...valide, nom: "D" })).toContain("nom");
    expect(champsEnErreur(registerSchema, { ...valide, prenom: "J" })).toContain("prenom");
  });

  it.each([
    ["trop court", "Court1A"],
    ["sans majuscule", "motdepasse123"],
    ["sans minuscule", "MOTDEPASSE123"],
    ["sans chiffre", "Motdepassesans"],
  ])("refuse un mot de passe %s", (_cas, motDePasse) => {
    const chemins = champsEnErreur(registerSchema, {
      ...valide,
      motDePasse,
      confirmation: motDePasse,
    });

    expect(chemins).toContain("motDePasse");
  });

  it("refuse une confirmation différente du mot de passe", () => {
    expect(champsEnErreur(registerSchema, { ...valide, confirmation: "Autre123456789" })).toEqual(
      ["confirmation"],
    );
  });
});

describe("otpSchema", () => {
  it("accepte un code à six chiffres", () => {
    expect(estValide(otpSchema, { code: "123456" })).toBe(true);
  });

  it.each(["12345", "1234567", "abcdef", "12 456", ""])("refuse le code %j", (code) => {
    expect(estValide(otpSchema, { code })).toBe(false);
  });
});

describe("beneficiarySchema", () => {
  it("normalise l'IBAN : espaces retirés et majuscules", () => {
    const resultat = beneficiarySchema.safeParse({
      nom: "Sara Alami",
      iban: "fr76 3000 4028 3798 7654 3210 985",
    });

    expect(resultat.success && resultat.data.iban).toBe("FR7630004028379876543210985");
  });

  it("accepte une banque renseignée ou vide", () => {
    const base = { nom: "Sara", iban: "FR7630004028379876543210985" };

    expect(estValide(beneficiarySchema, { ...base, banque: "Attijariwafa" })).toBe(true);
    expect(estValide(beneficiarySchema, { ...base, banque: "" })).toBe(true);
  });

  it("refuse un IBAN invalide", () => {
    expect(champsEnErreur(beneficiarySchema, { nom: "Sara", iban: "123" })).toContain("iban");
  });

  it("refuse un nom trop court", () => {
    expect(
      champsEnErreur(beneficiarySchema, { nom: "S", iban: "FR7630004028379876543210985" }),
    ).toContain("nom");
  });
});

describe("transferSchema", () => {
  const valide = {
    beneficiaireId: 5,
    montant: "150.50",
    motif: "Loyer",
    cleIdempotence: UUID,
  };

  it("accepte un virement valide", () => {
    expect(estValide(transferSchema, valide)).toBe(true);
  });

  it("accepte un virement sans motif", () => {
    const { motif: _motif, ...sansMotif } = valide;

    expect(estValide(transferSchema, sansMotif)).toBe(true);
  });

  it.each([0, -1, 1.5, "5"])("refuse l'identifiant de bénéficiaire %j", (beneficiaireId) => {
    expect(champsEnErreur(transferSchema, { ...valide, beneficiaireId })).toContain(
      "beneficiaireId",
    );
  });

  it.each(["0", "abc", "-5", "10.123", "100000.01", ""])("refuse le montant %j", (montant) => {
    expect(champsEnErreur(transferSchema, { ...valide, montant })).toContain("montant");
  });

  it.each(["1", "0.01", "100000", "99999.99"])("accepte le montant %j", (montant) => {
    expect(estValide(transferSchema, { ...valide, montant })).toBe(true);
  });

  it("limite le motif à 140 caractères", () => {
    expect(estValide(transferSchema, { ...valide, motif: "a".repeat(140) })).toBe(true);
    expect(champsEnErreur(transferSchema, { ...valide, motif: "a".repeat(141) })).toContain(
      "motif",
    );
  });

  it("exige une clé d'idempotence au format UUID", () => {
    expect(champsEnErreur(transferSchema, { ...valide, cleIdempotence: "abc" })).toContain(
      "cleIdempotence",
    );
  });
});

describe("claimSchema", () => {
  const valide = { objet: "Objet", description: "a".repeat(20) };

  it("accepte une réclamation aux longueurs minimales", () => {
    expect(estValide(claimSchema, valide)).toBe(true);
  });

  it("refuse un objet trop court ou trop long", () => {
    expect(champsEnErreur(claimSchema, { ...valide, objet: "abcd" })).toContain("objet");
    expect(champsEnErreur(claimSchema, { ...valide, objet: "a".repeat(121) })).toContain(
      "objet",
    );
  });

  it("refuse une description trop courte", () => {
    expect(champsEnErreur(claimSchema, { ...valide, description: "a".repeat(19) })).toContain(
      "description",
    );
  });
});

describe("alias français", () => {
  it("pointent vers les mêmes schémas", () => {
    expect(schemaConnexion).toBe(loginSchema);
    expect(schemaInscription).toBe(registerSchema);
    expect(schemaOtp).toBe(otpSchema);
    expect(schemaBeneficiaire).toBe(beneficiarySchema);
    expect(schemaVirement).toBe(transferSchema);
    expect(schemaReclamation).toBe(claimSchema);
  });
});
