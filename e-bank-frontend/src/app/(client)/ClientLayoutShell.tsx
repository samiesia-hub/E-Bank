"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/tableau-de-bord", label: "Tableau de bord" },
  { href: "/comptes", label: "Mes comptes" },
  { href: "/virements/nouveau", label: "Virement" },
  { href: "/beneficiaires", label: "Bénéficiaires" },
  { href: "/reclamations", label: "Réclamations" },
  { href: "/profil", label: "Profil" },
];

export default function ClientLayoutShell({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const pathname = usePathname();

  if (pathname === "/tableau-de-bord") {
    return children;
  }

  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-64 shrink-0 border-r border-divider bg-surface-container-lowest md:block">
        <div className="bg-navy px-space-lg py-space-md font-headline-sm text-headline-sm font-semibold text-white">
          Ora Bank
        </div>
        <nav className="flex flex-col gap-1 p-space-md">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="rounded-lg px-space-md py-2 font-body-md text-body-md text-on-surface hover:bg-surface-tint"
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-divider bg-surface-container-lowest px-space-lg">
          <span className="font-label-md text-label-md text-neutral-core">
            Espace client
          </span>
          <Link
            href="/profil"
            className="font-label-md text-label-md text-primary-container"
          >
            Mon profil
          </Link>
        </header>
        <div className="flex-1">{children}</div>
      </div>
    </div>
  );
}
