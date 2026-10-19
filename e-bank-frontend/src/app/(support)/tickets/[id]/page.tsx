export const dynamic = "force-dynamic";

interface Props { params: Promise<{ id: string }> }
export default async function PageDetailTicket({ params }: Props) {
  const { id } = await params;
  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <h1 className="font-headline-md text-headline-md text-navy">Ticket #{id}</h1>
      <p className="mt-space-sm font-body-md text-body-md text-neutral-core">Répondre réclamation, MAJ statut, infos client autorisées.</p>
    </main>
  );
}
