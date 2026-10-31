import Link from "next/link";

// Public landing — first migration step from code.html hero.
// Full code.html sections (steps, services, security, FAQ) come next.
export default function Home() {
  return (
    <main>
      {/* HERO — navy overlay like code.html */}
      <section className="flex min-h-[480px] items-center justify-center bg-navy">
        <div className="mx-auto w-full max-w-[1200px] px-margin py-space-2xl text-center">
          <span className="mb-space-md inline-flex items-center gap-space-xs bg-white/10 px-3 py-1 font-label-sm text-label-sm uppercase tracking-wider text-white">
            <span className="h-1.5 w-1.5 rounded-full bg-[#8DB8E8]" />
            Services bancaires en ligne
          </span>
          <h1 className="mx-auto max-w-4xl font-display-hero text-display-hero-mobile text-white lg:text-display-hero">
            Banque en ligne : découvrez tout ce que Ora Bank peut faire pour vous
          </h1>
          <p className="mx-auto mt-space-md max-w-2xl font-body-lg text-body-lg text-[#E5E8EE]">
            Gérez vos comptes, virements SEPA, bénéficiaires et relevés — avec une sécurité de niveau bancaire (OTP, KYC, audit).
          </p>
          <div className="mt-space-xl flex flex-wrap items-center justify-center gap-space-md">
            <Link href="/inscription" className="rounded-lg bg-primary-container px-space-lg py-2 font-label-md text-label-md text-white hover:bg-[#2C5A92]">Ouvrir un compte</Link>
            <Link href="/connexion" className="rounded-lg bg-access-teal px-space-lg py-2 font-label-md text-label-md text-white hover:bg-[#307272]">Accès client</Link>
            <Link href="/tableau-de-bord" className="rounded-lg border border-white/40 px-space-lg py-2 font-label-md text-label-md text-white hover:bg-white/10">Démo tableau de bord</Link>
          </div>
        </div>
      </section>
      {/* QUICK LINKS → routes scaffolded from use case.png */}
      <section className="mx-auto grid max-w-[1200px] gap-gutter px-margin py-space-xl md:grid-cols-3">
        {[
          { href: "/tableau-de-bord", title: "Espace client", desc: "Soldes, comptes, historique, relevés PDF." },
          { href: "/virements/nouveau", title: "Virements SEPA", desc: "Wizard 4 étapes + OTP + reçu." },
          { href: "/tickets", title: "Support", desc: "Tickets, réclamations, suivi." },
          { href: "/admin/utilisateurs", title: "Administration", desc: "Utilisateurs, KYC, audit, rôles." },
          { href: "/services", title: "Services", desc: "Comptes, épargne, crédits (vitrine)." },
          { href: "/securite", title: "Sécurité", desc: "FGDR, OTP, opposition carte." },
        ].map((c) => (
          <Link key={c.href + c.title} href={c.href} className="rounded-lg border border-divider bg-surface-container-lowest p-space-lg hover:bg-surface-tint">
            <h2 className="font-headline-sm text-headline-sm text-navy">{c.title}</h2>
            <p className="mt-space-sm font-body-md text-body-md text-neutral-core">{c.desc}</p>
          </Link>
        ))}
      </section>
    </main>
  );
}
