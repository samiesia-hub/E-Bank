"use client";

import { User, Mail, Phone, MapPin, Hash } from "lucide-react";
import { Carte } from "@/components/interface/Carte";
import { BanniereErreur } from "@/components/interface/BanniereErreur";
import { SqueletteLigne } from "@/components/interface/Squelettes";
import { useProfilClient } from "@/features/client/hooks";

function InfoLigne({
  icone: Icone,
  libelle,
  valeur,
}: {
  icone: React.ElementType;
  libelle: string;
  valeur: string | null | undefined;
}) {
  return (
    <div className="flex items-start gap-space-md py-space-md">
      <span className="mt-0.5 shrink-0 text-on-surface-variant">
        <Icone size={18} aria-hidden="true" />
      </span>
      <div>
        <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
          {libelle}
        </p>
        <p className="mt-space-xs font-body-md text-body-md text-on-surface">
          {valeur ?? <span className="italic text-neutral-core">Non renseigné</span>}
        </p>
      </div>
    </div>
  );
}

export default function PageProfil() {
  const { data: profil, isLoading, isError, error, refetch } = useProfilClient();

  return (
    <main className="mx-auto max-w-[800px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Ora Bank — Mon profil
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
        Mon profil
      </h1>
      <p className="mt-space-sm font-body-md text-body-md text-neutral-core">
        Vos informations personnelles enregistrées auprès de la banque.
      </p>

      {isError && (
        <div className="mt-space-lg">
          <BanniereErreur erreur={error} reessayer={refetch} />
        </div>
      )}

      <Carte className="mt-space-xl">
        {isLoading && (
          <div className="space-y-space-lg py-space-md">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-start gap-space-md">
                <SqueletteLigne className="mt-0.5 h-5 w-5 shrink-0 rounded" />
                <div className="flex-1 space-y-space-xs">
                  <SqueletteLigne className="h-3 w-24" />
                  <SqueletteLigne className="h-5 w-48" />
                </div>
              </div>
            ))}
          </div>
        )}

        {!isLoading && !isError && profil && (
          <>
            {/* Avatar / en-tête */}
            <div className="flex items-center gap-space-lg border-b border-divider pb-space-lg">
              <span className="grid h-16 w-16 place-items-center rounded-full bg-primary-fixed/30 text-primary">
                <User size={32} aria-hidden="true" />
              </span>
              <div>
                <p className="font-headline-sm text-headline-sm font-semibold text-navy">
                  {profil.nom}
                </p>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                  Client Ora Bank
                </p>
              </div>
            </div>

            {/* Champs */}
            <div className="divide-y divide-divider">
              <InfoLigne icone={Hash} libelle="Numéro client" valeur={profil.numeroClient} />
              <InfoLigne icone={Mail} libelle="Adresse e-mail" valeur={profil.email} />
              <InfoLigne icone={Phone} libelle="Téléphone" valeur={profil.telephone} />
              <InfoLigne icone={MapPin} libelle="Adresse postale" valeur={profil.adresse} />
            </div>

            {/* Note sécurité */}
            <p className="mt-space-lg rounded-lg bg-surface-container p-space-md font-body-sm text-body-sm text-neutral-core">
              Pour modifier vos informations, veuillez contacter votre conseiller bancaire.
            </p>
          </>
        )}
      </Carte>
    </main>
  );
}
