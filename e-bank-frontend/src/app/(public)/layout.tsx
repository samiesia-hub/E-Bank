import Link from "next/link";

// Marketing layout — reprend la structure code.html : header navy + footer navy
export default function MiseEnPagePublique({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="bg-navy text-white">
        <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between px-margin">
          <Link href="/" className="font-headline-sm text-headline-sm font-semibold">
            Ora Bank
          </Link>
          <nav className="hidden items-center gap-space-lg md:flex">
            <Link href="/services" className="font-body-sm text-body-sm text-white/80 hover:text-accent-yellow">Services</Link>
            <Link href="/securite" className="font-body-sm text-body-sm text-white/80 hover:text-accent-yellow">Sécurité</Link>
            <Link href="/faq" className="font-body-sm text-body-sm text-white/80 hover:text-accent-yellow">Aide</Link>
            <Link href="/connexion" className="rounded-lg bg-access-teal px-4 py-2 font-label-md text-label-md text-white hover:bg-[#307272]">Accès client</Link>
            <Link href="/inscription" className="rounded-lg border border-white/40 px-4 py-2 font-label-md text-label-md hover:bg-white/10">Devenir client</Link>
          </nav>
        </div>
      </header>
      <div className="flex-1">{children}</div>
      <footer className="bg-navy text-white">
        <div className="mx-auto grid max-w-[1200px] gap-gutter px-margin py-space-xl md:grid-cols-4">
          <div>
            <h4 className="font-label-md text-label-md uppercase tracking-wider text-white/50">Banque au quotidien</h4>
            <ul className="mt-space-md space-y-space-sm font-body-sm text-body-sm text-white/80">
              <li><Link href="/services">Comptes et cartes</Link></li>
              <li><Link href="/virements/nouveau">Virements &amp; SEPA</Link></li>
              <li><Link href="/services">Services digitaux</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="font-label-md text-label-md uppercase tracking-wider text-white/50">Sécurité &amp; Aide</h4>
            <ul className="mt-space-md space-y-space-sm font-body-sm text-body-sm text-white/80">
              <li><Link href="/securite">Centre sécurité</Link></li>
              <li><Link href="/faq">FAQ</Link></li>
              <li><Link href="/connexion">Opposition carte</Link></li>
            </ul>
          </div>
          <div className="md:col-span-2">
            <p className="font-body-sm text-body-sm text-white/60">Vos avoirs sont couverts par le FGDR conformément aux réglementations de l&apos;UE.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export { MiseEnPagePublique as PublicLayout };
