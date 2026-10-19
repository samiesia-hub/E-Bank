// FAQ / Assistance — voir use case.png / class diagram
import Link from "next/link";

export default function Page() {
  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">Bankati — FAQ / Assistance</p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">FAQ / Assistance</h1>
      <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">Questions fréquentes + contact conseillers.</p>
      <div className="mt-space-lg rounded-lg border border-divider bg-surface-container-lowest p-space-lg">
        <p className="font-body-sm text-body-sm text-neutral-core">TODO: brancher TanStack Query + API backend. Maquette en attente.</p>
        <Link href="/" className="mt-space-md inline-block font-label-md text-label-md text-primary-container hover:underline">← Retour accueil</Link>
      </div>
    </main>
  );
}
