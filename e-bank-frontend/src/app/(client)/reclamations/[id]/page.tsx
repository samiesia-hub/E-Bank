export const dynamic = "force-dynamic";

interface Props { params: Promise<{ id: string }> }
export default async function PageDetailReclamation({ params }: Props) {
  const { id } = await params;
  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <h1 className="font-headline-md text-headline-md text-navy">Réclamation #{id}</h1>
      <p className="mt-space-sm font-body-md text-body-md text-neutral-core">Suivi statut (OUVERTE → EN_COURS → RESOLUE → FERMEE) + réponses.</p>
    </main>
  );
}
