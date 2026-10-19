import Link from "next/link";

// Support portal — AGENT_SUPPORT + ADMIN (see src/proxy.ts)
export default function MiseEnPageSupport({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-64 shrink-0 border-r border-divider bg-surface-container-lowest md:block">
        <div className="bg-navy px-space-lg py-space-md font-headline-sm text-headline-sm font-semibold text-white">Bankati — Support</div>
        <nav className="flex flex-col gap-1 p-space-md">
          <Link href="/tickets" className="rounded-lg px-space-md py-2 font-body-md text-body-md hover:bg-surface-tint">Tickets</Link>
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center border-b border-divider bg-surface-container-lowest px-space-lg">
          <span className="font-label-md text-label-md text-neutral-core">Espace agent support</span>
        </header>
        <div className="flex-1">{children}</div>
      </div>
    </div>
  );
}

export { MiseEnPageSupport as SupportLayout };
