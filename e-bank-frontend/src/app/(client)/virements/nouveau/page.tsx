"use client";

import { useState } from "react";
import Link from "next/link";
import { Send, CheckCircle, AlertCircle } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteCarteCompte } from "@/components/interface/Squelettes";
import { useComptes, useEffectuerVirement } from "@/features/client/hooks";
import { messageErreurApi } from "@/lib/client-api";
import type { VirementEffectue } from "@/services/api";

type Etape = "formulaire" | "succes";

const CHAMP =
  "mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary";

export default function PageNouveauVirement() {
  const { data: comptes, isLoading, isError, error } = useComptes();
  const virement = useEffectuerVirement();

  const [etape, setEtape] = useState<Etape>("formulaire");
  const [recu, setRecu] = useState<VirementEffectue | null>(null);

  const [compteId, setCompteId] = useState("");
  const [numeroBeneficiaire, setNumeroBeneficiaire] = useState("");
  const [montant, setMontant] = useState("");
  const [motif, setMotif] = useState("");

  function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!compteId || !numeroBeneficiaire || !montant) return;
    virement.mutate(
      {
        compteSourceId: Number(compteId),
        numeroCompteBeneficiaire: numeroBeneficiaire,
        montant: Number(montant),
        motif: motif.trim(),
      },
      {
        onSuccess: (resultat) => {
          setRecu(resultat);
          setEtape("succes");
        },
      }
    );
  }

  function reinitialiser() {
    setEtape("formulaire");
    setRecu(null);
    setCompteId("");
    setNumeroBeneficiaire("");
    setMontant("");
    setMotif("");
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
            {recu
              ? `${new Intl.NumberFormat("fr-FR", {
                  style: "currency",
                  currency: "MAD",
                }).format(recu.montant)} envoyés à ${recu.beneficiaire}.`
              : "Votre virement est en cours de traitement."}
          </p>
          {recu && (
            <p className="mt-space-sm font-body-sm text-body-sm text-neutral-core">
              Référence {recu.reference} · statut {recu.statut}
            </p>
          )}
          <button
            onClick={reinitialiser}
            className="mt-space-lg rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
          >
            Nouveau virement
          </button>
          <Link
            href="/comptes"
            className="mt-space-sm font-label-md text-label-md text-primary hover:underline"
          >
            Voir mes comptes
          </Link>
        </Carte>
      </main>
    );
  }

  const formulaireIncomplet = !compteId || !numeroBeneficiaire || !montant;

  return (
    <main className="mx-auto max-w-[700px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Virement
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Effectuer un virement
      </h1>

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
                className={CHAMP}
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

          {/* Compte bénéficiaire */}
          <div>
            <label
              htmlFor="beneficiaire"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              RIB ou numéro de compte du bénéficiaire
            </label>
            <input
              id="beneficiaire"
              type="text"
              inputMode="numeric"
              maxLength={28}
              placeholder="24 chiffres (RIB), 13 chiffres (compte) ou IBAN"
              value={numeroBeneficiaire}
              onChange={(e) =>
                setNumeroBeneficiaire(
                  e.target.value.replace(/[^0-9A-Za-z]/g, "").toUpperCase()
                )
              }
              required
              className={`${CHAMP} tracking-widest`}
            />
            <p className="mt-space-sm font-body-sm text-body-sm text-neutral-core">
              {numeroBeneficiaire.length === 0
                ? "Le compte doit exister chez Orabank."
                : `${numeroBeneficiaire.length} caractères saisis.`}
            </p>
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
              className={CHAMP}
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
              className={CHAMP}
            />
          </div>

          {/* Erreur soumission */}
          {virement.error && (
            <div className="flex items-start gap-space-sm rounded-lg border border-[#f5c6c6] bg-[#fff5f5] p-space-md">
              <AlertCircle className="mt-0.5 shrink-0 text-[#c0392b]" size={18} />
              <p className="font-body-sm text-body-sm text-[#c0392b]">
                {messageErreurApi(virement.error)}
              </p>
            </div>
          )}

          <button
            type="submit"
            disabled={virement.isPending || isLoading || formulaireIncomplet}
            className="inline-flex w-full items-center justify-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-3 font-label-md text-label-md text-white hover:bg-primary disabled:cursor-not-allowed disabled:opacity-50"
          >
            {virement.isPending ? (
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
