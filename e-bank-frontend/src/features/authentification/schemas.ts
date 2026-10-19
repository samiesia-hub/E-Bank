import { z } from "zod";

// Shared with backend rules: IBAN shape, positive amount, password policy.
const ibanShape = /^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/;

export const loginSchema = z.object({
  email: z.string().email("Email invalide."),
  motDePasse: z.string().min(1, "Mot de passe requis."),
});

export const registerSchema = z
  .object({
    nom: z.string().min(2, "Nom requis."),
    prenom: z.string().min(2, "Prénom requis."),
    email: z.string().email("Email invalide."),
    telephone: z.string().min(6, "Téléphone invalide.").optional().or(z.literal("")),
    motDePasse: z
      .string()
      .min(12, "12 caractères minimum.")
      .regex(/[A-Z]/, "Une majuscule requise.")
      .regex(/[a-z]/, "Une minuscule requise.")
      .regex(/\d/, "Un chiffre requis."),
    confirmation: z.string(),
  })
  .refine((v) => v.motDePasse === v.confirmation, {
    message: "Les mots de passe ne correspondent pas.",
    path: ["confirmation"],
  });

export const otpSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Code à 6 chiffres."),
});

export const beneficiarySchema = z.object({
  nom: z.string().min(2, "Nom requis."),
  iban: z
    .string()
    .transform((v) => v.replace(/\s+/g, "").toUpperCase())
    .pipe(z.string().regex(ibanShape, "IBAN invalide.")),
  banque: z.string().optional().or(z.literal("")),
});

export const transferSchema = z.object({
  beneficiaireId: z.number().int().positive("Choisissez un bénéficiaire."),
  montant: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, "Montant invalide.")
    .refine((v) => Number(v) > 0, "Montant supérieur à 0 requis.")
    .refine((v) => Number(v) <= 100000, "Plafond par virement dépassé (100 000)."),
  motif: z.string().max(140, "140 caractères max.").optional().or(z.literal("")),
  // Idempotency key generated client-side per wizard run — prevents double debit
  cleIdempotence: z.string().uuid("Clé idempotence manquante."),
});

export const claimSchema = z.object({
  objet: z.string().min(5, "Objet requis (5 caractères min).").max(120),
  description: z.string().min(20, "Décrivez le problème (20 caractères min)."),
});

export type LoginInput = z.infer<typeof loginSchema>;
export type RegisterInput = z.infer<typeof registerSchema>;
export type TransferInput = z.infer<typeof transferSchema>;

// Alias français (noms anglais conservés pour compatibilité)
export {
  loginSchema as schemaConnexion,
  registerSchema as schemaInscription,
  otpSchema as schemaOtp,
  beneficiarySchema as schemaBeneficiaire,
  transferSchema as schemaVirement,
  claimSchema as schemaReclamation,
};
export type { LoginInput as EntreeConnexion, RegisterInput as EntreeInscription, TransferInput as EntreeVirement };
