import Image from "next/image";
import Link from "next/link";
import {
  Bell,
  CircleHelp,
  FileText,
  Landmark,
  LayoutDashboard,
  LogOut,
  Send,
  Settings,
  UserRound,
  UsersRound,
} from "lucide-react";

const links = [
  { href: "/tableau-de-bord", label: "Tableau de bord", icon: LayoutDashboard },
  { href: "/comptes", label: "Mes comptes", icon: Landmark },
  { href: "/virements/nouveau", label: "Virements", icon: Send },
  { href: "/beneficiaires", label: "Bénéficiaires", icon: UsersRound },
  { href: "/reclamations", label: "Réclamations", icon: FileText },
];

export default function MiseEnPageClient({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-[#f5f7fb] text-on-surface">
      <header className="sticky top-0 z-20 flex h-[72px] items-center justify-between border-b border-white/10 bg-navy px-5 text-white md:px-8">
        <Link href="/tableau-de-bord" className="flex items-center gap-3" aria-label="Ora Bank, tableau de bord">
          <Image src="/logo-ora-bank-icone.png" alt="Ora Bank" width={289} height={153} priority className="h-9 w-auto" />
          <span className="hidden border-l border-white/20 pl-3 text-sm font-medium tracking-wide sm:block">Espace client</span>
        </Link>
        <div className="flex items-center gap-2 sm:gap-4">
          <button type="button" aria-label="Notifications" className="relative grid h-10 w-10 place-items-center rounded-full transition hover:bg-white/10">
            <Bell size={20} aria-hidden="true" />
            <span className="absolute right-2 top-2 h-2 w-2 rounded-full border border-navy bg-accent-yellow" />
          </button>
          <Link href="/profil" className="flex items-center gap-2 rounded-full py-1 pr-1 transition hover:bg-white/10 sm:pl-2">
            <span className="hidden text-right sm:block"><span className="block text-sm font-semibold"></span><span className="block text-xs text-white/65">Particulier</span></span>
            <span className="grid h-9 w-9 place-items-center rounded-full bg-access-teal text-sm font-semibold"></span>
          </Link>
        </div>
      </header>

      <div className="mx-auto flex max-w-[1440px]">
        <aside className="sticky top-[72px] hidden h-[calc(100vh-72px)] w-[248px] shrink-0 flex-col border-r border-divider bg-white p-4 lg:flex">
          <p className="px-3 pb-3 pt-1 text-xs font-semibold uppercase tracking-[0.14em] text-neutral-core">Mon espace</p>
          <nav className="space-y-1" aria-label="Navigation client">
            {links.map(({ href, label, icon: Icon }) => (
              <Link key={href} href={href} className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#4c566b] transition hover:bg-[#edf3fc] hover:text-primary">
                <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
                {label}
              </Link>
            ))}
          </nav>
          <div className="mt-6 border-t border-divider pt-5">
            <Link href="/profil" className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#4c566b] transition hover:bg-[#edf3fc] hover:text-primary"><UserRound size={19} aria-hidden="true" /> Mon profil</Link>
            <Link href="/profil" className="mt-1 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#4c566b] transition hover:bg-[#edf3fc] hover:text-primary"><Settings size={19} aria-hidden="true" /> Paramètres</Link>
          </div>
          <div className="mt-auto rounded-xl bg-[#f3f6fb] p-4">
            <CircleHelp size={20} className="text-primary" aria-hidden="true" />
            <p className="mt-3 text-sm font-semibold text-navy">Besoin d&apos;aide ?</p>
            <p className="mt-1 text-xs leading-5 text-neutral-core">Notre assistance est disponible 24h/24 et 7j/7.</p>
            <Link href="/reclamations" className="mt-3 inline-block text-xs font-semibold text-primary hover:underline">Nous contacter</Link>
          </div>
          <button type="button" className="mt-3 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#4c566b] transition hover:bg-red-50 hover:text-error"><LogOut size={19} aria-hidden="true" /> Déconnexion</button>
        </aside>
        <main className="min-w-0 flex-1">{children}</main>
      </div>

      <nav className="fixed inset-x-0 bottom-0 z-30 flex justify-around border-t border-divider bg-white px-2 py-2 shadow-[0_-4px_16px_rgba(11,31,75,0.06)] lg:hidden" aria-label="Navigation mobile">
        {links.map(({ href, label, icon: Icon }) => (
          <Link key={href} href={href} className="flex min-w-0 flex-col items-center gap-1 px-1 text-[10px] font-medium text-[#596276] hover:text-primary"><Icon size={19} aria-hidden="true" /><span className="truncate">{label}</span></Link>
        ))}
      </nav>
    </div>
  );
}

export { MiseEnPageClient as ClientLayout };
