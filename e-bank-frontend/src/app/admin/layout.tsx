import Link from "next/link";

// Admin portal — ADMIN only (see src/proxy.ts)
const links = [
  { href: "/admin/utilisateurs", label: "Utilisateurs" },
  { href: "/admin/comptes-bancaires", label: "Comptes" },
  { href: "/admin/transactions", label: "Transactions" },
  { href: "/admin/kyc", label: "KYC" },
  { href: "/admin/audit", label: "Audit" },
  { href: "/admin/roles-permissions", label: "Rôles" },
  { href: "/admin/employes-support", label: "Support" },
];

export default function MiseEnPageAdmin({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-64 shrink-0 border-r border-divider bg-surface-container-lowest md:block">
        <div className="bg-navy px-space-lg py-space-md font-headline-sm text-headline-sm font-semibold text-white">Bankati — Admin</div>
        <nav className="flex flex-col gap-1 p-space-md">
          {links.map((l) => (
            <Link key={l.href} href={l.href} className="rounded-lg px-space-md py-2 font-body-md text-body-md hover:bg-surface-tint">
              {l.label}
            </Link>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center border-b border-divider bg-surface-container-lowest px-space-lg">
          <span className="font-label-md text-label-md text-neutral-core">Administration</span>
        </header>
        <div className="flex-1">{children}</div>
      </div>
    </div>
  );
}

export { MiseEnPageAdmin as AdminLayout };
