"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, Trash2, UserPlus, AlertCircle } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import {
  useBeneficiaires,
  useCreerBeneficiaire,
  useSupprimerBeneficiaire,
} from "@/features/client/hooks";
import { messageErreurApi } from "@/lib/client-api";

const CHAMP =
  "mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary";

export default function PageBeneficiaires() {
  const { data: beneficiaires, isLoading, isError, error } = useBeneficiaires();
  const ajout = useCreerBeneficiaire();
  const suppression = useSupprimerBeneficiaire();

  const [formulaireOuvert, setFormulaireOuvert] = useState(false);
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [numeroCompte, setNumeroCompte] = useState("");

  const liste = beneficiaires ?? [];
  const incomplet = !nom || !prenom || !numeroCompte;

  function ajouter(e: React.FormEvent) {
    e.preventDefault();
    if (incomplet) return;
    ajout.mutate(
      {
        nom: nom.trim(),
        prenom: prenom.trim(),
        numeroCompte: numeroCompte.replace(/[^0-9A-Za-z]/g, "").toUpperCase(),
      },
      {
        onSuccess: () => {
          setNom("");
          setPrenom("");
          setNumeroCompte("");
          setFormulaireOuvert(false);
        },
      }
    );
  }

  return (
    <main className="mx-auto max-w-[900px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Orabank — Bénéficiaires
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Bénéficiaires
      </h1>
      <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
        Les personnes vers qui vous pouvez envoyer un virement.
      </p>

      <div className="mt-space-lg flex flex-wrap gap-space-md">
        <button
          type="button"
          onClick={() => setFormulaireOuvert((ouvert) => !ouvert)}
          className="inline-flex items-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
        >
          <Plus size={16} /> Ajouter un bénéficiaire
        </button>
        <Link
          href="/virements/nouveau"
          className="inline-flex items-center gap-space-sm rounded-lg border border-primary px-space-lg py-2 font-label-md text-label-md text-primary hover:bg-surface-container"
        >
          Faire un virement
        </Link>
      </div>

      {isError && (
        <div className="mt-space-lg">
          <BanniereErreur erreur={error} />
        </div>
      )}

      {formulaireOuvert && (
        <Carte className="mt-space-lg">
          <form onSubmit={ajouter} className="space-y-space-md" noValidate>
            <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
              Nouveau bénéficiaire
            </h2>
            <div className="grid gap-space-md sm:grid-cols-2">
              <div>
                <label htmlFor="prenom" className="block font-label-md text-label-md font-semibold text-on-surface">
                  Prénom
                </label>
                <input
                  id="prenom"
                  type="text"
                  value={prenom}
                  onChange={(e) => setPrenom(e.target.value)}
                  required
                  className={CHAMP}
                />
              </div>
              <div>
                <label htmlFor="nom" className="block font-label-md text-label-md font-semibold text-on-surface">
                  Nom
                </label>
                <input
                  id="nom"
                  type="text"
                  value={nom}
                  onChange={(e) => setNom(e.target.value)}
                  required
                  className={CHAMP}
                />
              </div>
            </div>
            <div>
              <label htmlFor="numeroCompte" className="block font-label-md text-label-md font-semibold text-on-surface">
                RIB ou IBAN du bénéficiaire
              </label>
              <input
                id="numeroCompte"
                type="text"
                maxLength={28}
                placeholder="RIB 24 chiffres, ou IBAN MA suivi de 26 chiffres"
                value={numeroCompte}
                onChange={(e) =>
                  setNumeroCompte(e.target.value.replace(/[^0-9A-Za-z]/g, "").toUpperCase())
                }
                required
                className={`${CHAMP} tracking-widest`}
              />
            </div>

            {ajout.error && (
              <div className="flex items-start gap-space-sm rounded-lg border border-[#f5c6c6] bg-[#fff5f5] p-space-md">
                <AlertCircle className="mt-0.5 shrink-0 text-[#c0392b]" size={18} />
                <p className="font-body-sm text-body-sm text-[#c0392b]">
                  {messageErreurApi(ajout.error)}
                </p>
              </div>
            )}

            <button
              type="submit"
              disabled={ajout.isPending || incomplet}
              className="inline-flex items-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserPlus size={16} />
              {ajout.isPending ? "Ajout…" : "Enregistrer"}
            </button>
          </form>
        </Carte>
      )}

      <Carte className="mt-space-lg">
        {isLoading && (
          <p className="font-body-md text-body-md text-neutral-core">Chargement…</p>
        )}

        {!isLoading && liste.length === 0 && (
          <p className="font-body-md text-body-md text-neutral-core">
            Aucun bénéficiaire enregistré pour le moment.
          </p>
        )}

        {liste.length > 0 && (
          <ul className="divide-y divide-divider">
            {liste.map((b) => (
              <li key={b.id} className="flex flex-wrap items-center justify-between gap-space-md py-space-md">
                <div>
                  <p className="font-label-md text-label-md font-semibold text-on-surface">
                    {b.prenom} {b.nom}
                  </p>
                  <p className="font-body-sm text-body-sm text-neutral-core tracking-wider">
                    {b.numeroCompte}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => suppression.mutate(b.id)}
                  disabled={suppression.isPending}
                  aria-label={`Supprimer ${b.prenom} ${b.nom}`}
                  className="inline-flex items-center gap-space-sm rounded-lg border border-divider px-space-md py-2 font-label-md text-label-md text-[#c0392b] hover:bg-[#fff5f5] disabled:opacity-50"
                >
                  <Trash2 size={16} /> Supprimer
                </button>
              </li>
            ))}
          </ul>
        )}

        {suppression.error && (
          <p className="mt-space-md font-body-sm text-body-sm text-[#c0392b]">
            {messageErreurApi(suppression.error)}
          </p>
        )}
      </Carte>
    </main>
  );
}
