"use client";

import { useState } from "react";
import Link from "next/link";
import { FileWarning, Plus, ChevronRight } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteLigne } from "@/components/interface/Squelettes";
import { useReclamations, useCreerReclamation } from "@/features/client/hooks";
import { messageErreurApi } from "@/lib/client-api";
import { formaterDate } from "@/lib/formatage";

const couleursStatut: Record<string, string> = {
  OUVERTE: "bg-[#fff1ed] text-[#d85d3f]",
  EN_COURS: "bg-[#e8f4ff] text-[#1565c0]",
  RESOLUE: "bg-[#e8f8f0] text-[#238b5c]",
  FERMEE: "bg-surface-container text-on-surface-variant",
};

export default function PageReclamations() {
  const { data: reclamations, isLoading, isError, error, refetch } = useReclamations();
  const { mutate, isPending, error: erreurCreation, isSuccess, reset } = useCreerReclamation();

  const [afficherFormulaire, setAfficherFormulaire] = useState(false);
  const [objet, setObjet] = useState("");
  const [message, setMessage] = useState("");

  function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!objet.trim() || !message.trim()) return;
    mutate(
      { objet: objet.trim(), message: message.trim() },
      {
        onSuccess: () => {
          setObjet("");
          setMessage("");
          setAfficherFormulaire(false);
        },
      }
    );
  }

  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      {/* En-tête */}
      <div className="flex flex-wrap items-end justify-between gap-space-md">
        <div>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
            Ora Bank — Réclamations
          </p>
          <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
            Mes réclamations
          </h1>
          <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
            Créez et suivez vos réclamations auprès du service client.
          </p>
        </div>

        <button
          onClick={() => { setAfficherFormulaire((v) => !v); reset(); }}
          className="inline-flex items-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
        >
          <Plus size={16} />
          Nouvelle réclamation
        </button>
      </div>

      {/* Formulaire de création */}
      {afficherFormulaire && (
        <Carte className="mt-space-xl">
          <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
            Nouvelle réclamation
          </h2>
          <form onSubmit={soumettre} className="mt-space-lg space-y-space-lg" noValidate>
            <div>
              <label htmlFor="objet" className="block font-label-md text-label-md font-semibold text-on-surface">
                Objet
              </label>
              <input
                id="objet"
                type="text"
                maxLength={120}
                placeholder="ex. Frais bancaires contestés"
                value={objet}
                onChange={(e) => setObjet(e.target.value)}
                required
                className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <label htmlFor="message" className="block font-label-md text-label-md font-semibold text-on-surface">
                Description
              </label>
              <textarea
                id="message"
                rows={4}
                maxLength={1000}
                placeholder="Décrivez votre problème en détail…"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                required
                className="mt-space-sm w-full rounded-lg border border-divider bg-surface-container-lowest px-space-md py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>

            {erreurCreation && (
              <p className="font-body-sm text-body-sm text-[#c0392b]">
                {messageErreurApi(erreurCreation)}
              </p>
            )}

            <div className="flex gap-space-md">
              <button
                type="submit"
                disabled={isPending || !objet.trim() || !message.trim()}
                className="rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary disabled:opacity-50"
              >
                {isPending ? "Envoi…" : "Envoyer"}
              </button>
              <button
                type="button"
                onClick={() => setAfficherFormulaire(false)}
                className="rounded-lg border border-divider px-space-lg py-2 font-label-md text-label-md text-on-surface hover:bg-surface-container"
              >
                Annuler
              </button>
            </div>
          </form>
        </Carte>
      )}

      {/* Confirmation */}
      {isSuccess && !afficherFormulaire && (
        <div className="mt-space-lg rounded-lg border border-[#b7e4c7] bg-[#e8f8f0] p-space-md font-body-sm text-body-sm text-[#238b5c]">
          ✓ Réclamation envoyée — elle apparaît dans la liste ci-dessous.
        </div>
      )}

      {/* Erreur chargement */}
      {isError && (
        <div className="mt-space-lg">
          <BanniereErreur erreur={error} reessayer={refetch} />
        </div>
      )}

      {/* Liste skeletons */}
      <div className="mt-space-xl">
        {isLoading && (
          <div className="space-y-space-sm">
            {[1, 2, 3].map((i) => (
              <div key={i} className="rounded-xl border border-divider bg-surface-container-lowest p-space-lg">
                <div className="flex items-center justify-between gap-space-md">
                  <SqueletteLigne className="h-5 w-48" />
                  <SqueletteLigne className="h-5 w-20 rounded-full" />
                </div>
                <SqueletteLigne className="mt-space-sm h-4 w-32" />
              </div>
            ))}
          </div>
        )}

        {/* Aucune réclamation */}
        {!isLoading && !isError && reclamations && reclamations.length === 0 && (
          <Carte className="flex min-h-56 flex-col items-center justify-center text-center">
            <FileWarning className="text-[#9c6b06]" size={32} />
            <p className="mt-space-md font-label-md text-label-md font-semibold text-navy">
              Aucune réclamation
            </p>
            <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
              Utilisez le bouton « Nouvelle réclamation » pour soumettre votre demande.
            </p>
          </Carte>
        )}

        {/* Liste */}
        {!isLoading && !isError && reclamations && reclamations.length > 0 && (
          <ul className="space-y-space-sm">
            {reclamations.map((rec) => (
              <li key={rec.id}>
                <Link
                  href={`/reclamations/${rec.id}`}
                  className="group flex items-center justify-between gap-space-md rounded-xl border border-divider bg-surface-container-lowest p-space-lg transition-shadow hover:shadow-sm"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-label-md text-label-md font-semibold text-on-surface">
                      {rec.objet}
                    </p>
                    <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                      {formaterDate(rec.dateCreation)}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-space-md">
                    <span
                      className={`rounded-full px-2 py-1 font-label-sm text-label-sm ${
                        couleursStatut[rec.statut] ?? "bg-surface-container text-on-surface-variant"
                      }`}
                    >
                      {rec.statut.replace(/_/g, " ").toLowerCase()}
                    </span>
                    <ChevronRight size={16} className="text-neutral-core group-hover:text-primary" />
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
