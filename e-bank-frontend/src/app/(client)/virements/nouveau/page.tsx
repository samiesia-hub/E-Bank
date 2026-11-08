"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Send, CheckCircle, AlertCircle } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteCarteCompte } from "@/components/interface/Squelettes";
import { useComptes } from "@/features/client/hooks";
import { appelApi } from "@/lib/client-api";
import { messageErreurApi } from "@/lib/client-api";

// Payload POST /api/client/virements (endpoint à venir — implémenté ici en avance)
interface VirementPayload {
  compteDebiteurId: number;
  ribBeneficiaire: string;
  montant: number;
  motif: string;
}

type Etape = "formulaire" | "succes";

export default function PageNouveauVirement() {
  const { data: comptes, isLoading, isError, error } = useComptes();

  const [etape, setEtape] = useState<Etape>("formulaire");
  const [compteId, setCompteId] = useState("");
  const [rib, setRib] = useState("");
  const [montant, setMontant] = useState("");
  const [motif, setMotif] = useState("");

  const { mutate, isPending, error: erreurMutation } = useMutation({
    mutationFn: (payload: VirementPayload) =>
      appelApi<unknown>("/client/virements", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => setEtape("succes"),
  });

  function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!compteId || !rib || !montant) return;
    mutate({
      compteDebiteurId: Number(compteId),
      ribBeneficiaire: rib.replace(/\s/g, ""),
      montant: Number(montant),
      motif,
    });
  }

  if (etape === "succes") {
    return (
      <main className="mx-auto max-w-[600px] px-margin py-space-xl">
        <Carte className="flex flex-col items-center py-space-xl text-center">
          <CheckCircle className="text-[#238b5c]" size={48} />
          <h1 className="mt-space-lg font-headline-md text-headline-md text-navy">
            Virement envoyé
          </h1>
          <p className="mt-space-sm font-body-md text-body-md text-neutral-core">
            Votre virement est en cours de traitement.
          </p>
          <button
            onClick={() => {
              setEtape("formulaire");
              setRib(""); setMontant(""); setMotif(""); setCompteId("");
            }}
            className="mt-space-lg rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
          >
            Nouveau virement
          </button>
        </Carte>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-[700px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Virement
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Effectuer un virement
      </h1>

      {/* Erreur chargement comptes */}
      {isError && (
        <div className="mt-space-lg">
          <BanniereErreur erreur={error} />
        </div>
      )}

      <Carte className="mt-space-xl">
        <form onSubmit={soumettre} className="space-y-space-lg" noValidate>

          {/* Compte débiteur */}
          <div>
            <label
              htmlFor="compte"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Compte à débiter
            </label>

            {isLoading && <SqueletteCarteCompte />}

            {!isLoading && !isError && (
              <select
                id="compte"
                value={compteId}
                onChange={(e) => setCompteId(e.target.value)}
                required
                className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">— Choisir un compte —</option>
                {(comptes ?? [])
                  .filter((c) => c.statutCompte === "ACTIF")
                  .map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.typeCompte} — {new Intl.NumberFormat("fr-FR", {
                        style: "currency",
                        currency: c.devise ?? "MAD",
                      }).format(c.solde)} (RIB: {c.rib?.slice(-6)})
                    </option>
                  ))}
              </select>
            )}
          </div>

          {/* RIB bénéficiaire */}
          <div>
            <label
              htmlFor="rib"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              RIB bénéficiaire
            </label>
            <input
              id="rib"
              type="text"
              inputMode="numeric"
              maxLength={28}
              placeholder="24 chiffres"
              value={rib}
              onChange={(e) => setRib(e.target.value.replace(/\D/g, ""))}
              required
              className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface tracking-widest focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Montant */}
          <div>
            <label
              htmlFor="montant"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Montant (MAD)
            </label>
            <input
              id="montant"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="0.00"
              value={montant}
              onChange={(e) => setMontant(e.target.value)}
              required
              className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Motif */}
          <div>
            <label
              htmlFor="motif"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Motif <span className="font-body-sm text-neutral-core">(optionnel)</span>
            </label>
            <input
              id="motif"
              type="text"
              maxLength={120}
              placeholder="ex. Loyer octobre"
              value={motif}
              onChange={(e) => setMotif(e.target.value)}
              className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Erreur soumission */}
          {erreurMutation && (
            <div className="flex items-start gap-space-sm rounded-lg border border-[#f5c6c6] bg-[#fff5f5] p-space-md">
              <AlertCircle className="mt-0.5 shrink-0 text-[#c0392b]" size={18} />
              <p className="font-body-sm text-body-sm text-[#c0392b]">
                {messageErreurApi(erreurMutation)}
              </p>
            </div>
          )}

          <button
            type="submit"
            disabled={isPending || isLoading || !compteId || !rib || !montant}
            className="inline-flex w-full items-center justify-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-3 font-label-md text-label-md text-white hover:bg-primary disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isPending ? (
              <span className="animate-pulse">Envoi en cours…</span>
            ) : (
              <>
                <Send size={16} />
                Confirmer le virement
              </>
            )}
          </button>
        </form>
      </Carte>
    </main>
  );
}
