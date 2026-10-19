// Détail compte : solde + historique + relevé PDF
export const dynamic = "force-dynamic";

interface Props { params: Promise<{ id: string }> }
export default async function PageDetailCompte({ params }: Props) {
  const { id } = await params;
  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">Compte {id}</p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">Détail du compte</h1>
      <p className="mt-space-sm font-body-md text-body-md text-neutral-core">Historique des transactions, filtres, pagination, téléchargement relevé (Releve).</p>
    </main>
  );
}
