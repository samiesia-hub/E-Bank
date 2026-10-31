import Link from "next/link";
import {
  ArrowRight,
  Building2,
  FileWarning,
  Send,
  WalletCards,
} from "lucide-react";
import { Carte } from "@/components/interface/Carte";

export default function Page() {
  return (
    <main className="mx-auto max-w-[1200px] px-margin pb-24 pt-space-xl">
      <div className="flex flex-wrap items-end justify-between gap-space-md">
        <div>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
            Espace client
          </p>

          <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">
            Tableau de bord
          </h1>

          <p className="mt-space-sm font-body-md text-body-md text-neutral-core">
            Consultez vos comptes, vos transactions et vos réclamations.
          </p>
        </div>

        <Link
          href="/virements/nouveau"
          className="inline-flex items-center gap-space-sm rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-primary"
        >
          <Send size={17} />
          Faire un virement
        </Link>
      </div>

      <div className="mt-space-xl grid gap-gutter lg:grid-cols-[minmax(0,1fr)_300px]">
        <section>
          <div className="mb-space-md">
            <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
              Mes comptes
            </h2>

            <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
              Vos comptes et soldes apparaîtront ici.
            </p>
          </div>

          <Carte className="flex min-h-48 flex-col items-center justify-center text-center">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-surface-container text-primary">
              <Building2 size={23} />
            </span>

            <p className="mt-space-md font-label-md text-label-md font-semibold text-navy">
              Aucun compte à afficher
            </p>

            <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
              Les informations de vos comptes seront disponibles après connexion
              au service bancaire.
            </p>
          </Carte>
        </section>

        <section
          className="rounded-lg border border-[#f1d99c] bg-[#fffaf0] p-space-lg"
          aria-labelledby="titre-reclamations"
        >
          <FileWarning
            className="text-[#9c6b06]"
            size={24}
            aria-hidden="true"
          />

          <h2
            id="titre-reclamations"
            className="mt-space-md font-headline-sm text-headline-sm font-semibold text-[#67470a]"
          >
            Réclamations en cours
          </h2>

          <p className="mt-space-md text-3xl font-semibold text-[#67470a]">
            —
          </p>

          <p className="mt-space-sm font-body-sm text-body-sm text-[#806126]">
            Le nombre de réclamations sera affiché ici.
          </p>

          <Link
            href="/reclamations"
            className="mt-space-lg inline-block font-label-md text-label-md text-[#8d6109] hover:underline"
          >
            Voir mes réclamations
          </Link>
        </section>
      </div>

      <section className="mt-space-xl">
        <div className="mb-space-md flex items-end justify-between gap-space-md">
          <div>
            <h2 className="font-headline-sm text-headline-sm font-semibold text-navy">
              Dernières transactions
            </h2>

            <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
              Vos dernières opérations apparaîtront ici.
            </p>
          </div>

          <Link
            href="/comptes"
            className="inline-flex items-center gap-space-xs font-label-md text-label-md text-primary hover:underline"
          >
            Voir tout
            <ArrowRight size={16} />
          </Link>
        </div>

        <Carte className="flex min-h-52 flex-col items-center justify-center text-center">
          <span className="grid h-12 w-12 place-items-center rounded-full bg-surface-container text-primary">
            <WalletCards size={23} />
          </span>

          <p className="mt-space-md font-label-md text-label-md font-semibold text-navy">
            Aucune transaction à afficher
          </p>

          <p className="mt-space-xs max-w-sm font-body-sm text-body-sm text-neutral-core">
            Les transactions apparaîtront ici lorsque le service sera connecté.
          </p>
        </Carte>
      </section>
    </main>
  );
}