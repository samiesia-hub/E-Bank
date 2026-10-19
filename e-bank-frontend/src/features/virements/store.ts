import { create } from "zustand";

// Transfer wizard: bénéficiaire → montant → résumé → OTP → reçu
// Idempotency key per run: double-click "Confirmer" must never debit twice.
export type EtapeVirement = "beneficiaire" | "montant" | "resume" | "otp" | "recu";

interface EtatAssistantVirement {
  etape: EtapeVirement;
  beneficiaireId: number | null;
  montant: string;
  motif: string;
  cleIdempotence: string;
  definirEtape: (etape: EtapeVirement) => void;
  definirDetails: (d: { beneficiaireId: number; montant: string; motif: string }) => void;
  reinitialiser: () => void;
}

function nouvelleCle(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export const utiliserAssistantVirement = create<EtatAssistantVirement>((set) => ({
  etape: "beneficiaire",
  beneficiaireId: null,
  montant: "",
  motif: "",
  cleIdempotence: nouvelleCle(),
  definirEtape: (etape) => set({ etape }),
  definirDetails: (d) =>
    set({ beneficiaireId: d.beneficiaireId, montant: d.montant, motif: d.motif }),
  reinitialiser: () =>
    set({
      etape: "beneficiaire",
      beneficiaireId: null,
      montant: "",
      motif: "",
      cleIdempotence: nouvelleCle(),
    }),
}));

// Alias anglais pour compatibilité
export {
  utiliserAssistantVirement as useTransferWizard,
  type EtapeVirement as TransferStep,
  type EtatAssistantVirement as TransferWizardState,
};
