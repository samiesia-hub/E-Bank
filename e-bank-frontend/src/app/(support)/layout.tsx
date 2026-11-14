import Image from "next/image";
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
        <div className="flex items-center gap-3 bg-navy px-space-lg py-space-md">
          <Image
            src="/logo-ora-bank-clair.png"
            alt="Ora Bank"
            width={525}
            height={317}
            priority
            className="h-9 w-auto"
          />
          <span className="font-label-md text-label-md font-semibold uppercase tracking-wider text-ora-gold">
            Support
          </span>
        </div>
        <nav className="flex flex-col gap-1 p-space-md">
          <Link href="/tickets" className="rounded-lg px-space-md py-2 font-body-md text-body-md hover:bg-surface-tint">Tickets</Link>
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-3 border-b border-divider bg-surface-container-lowest px-space-lg">
          <Image
            src="/logo-ora-bank.png"
            alt="Ora Bank"
            width={525}
            height={317}
            priority
            className="h-8 w-auto md:hidden"
          />
          <span className="font-label-md text-label-md text-neutral-core">Espace agent support</span>
        </header>
        <div className="flex-1">{children}</div>
      </div>
    </div>
  );
}

export { MiseEnPageSupport as SupportLayout };
