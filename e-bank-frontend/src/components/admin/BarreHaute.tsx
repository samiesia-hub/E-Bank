import { Bell, Search } from "lucide-react";

// Composant serveur : pas d'état ni de hook, donc pas besoin de "use client".
export function BarreHaute() {
    return (
        <header className="sticky top-0 z-20 flex h-16 items-center bg-navy text-white">
            {/* Logo : même largeur que la sidebar (w-64) pour que tout s'aligne */}
            <div className="flex w-64 shrink-0 flex-col justify-center px-space-lg">
                <span className="text-headline-sm font-semibold">Bankati</span>
                <span className="text-label-sm uppercase tracking-wider text-white/70">
          Administration
        </span>
            </div>

            {/* Recherche globale */}
            <div className="flex-1 px-space-lg">
                <div className="relative max-w-xl">
                    <Search
                        size={18}
                        aria-hidden="true"
                        className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-white/60"
                    />
                    <input
                        type="search"
                        aria-label="Recherche globale"
                        placeholder="Rechercher un client, un compte, une transaction"
                        className="h-10 w-full rounded-lg border border-white/20 bg-white/10 pl-10 pr-3 text-body-sm text-white placeholder:text-white/60 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.4)]"
                    />
                </div>
            </div>

            {/* Notifications + profil */}
            <div className="flex items-center gap-space-md px-space-lg">
                <button
                    type="button"
                    aria-label="Notifications"
                    className="relative flex h-10 w-10 items-center justify-center rounded-lg transition-colors hover:bg-white/10"
                >
                    <Bell size={20} aria-hidden="true" />
                    <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-accent-yellow" />
                </button>

                {/* TODO Partie 7 : nom et rôle viennent de la session, pas du texte fixe */}
                <div className="flex items-center gap-3">
                    <div
                        aria-hidden="true"
                        className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-container text-label-md font-semibold"
                    >
                        AB
                    </div>
                    <div className="flex flex-col">
                        <span className="text-label-md font-semibold">Admin Bankati</span>
                        <span className="text-label-sm text-white/70">Administrateur</span>
                    </div>
                </div>
            </div>
        </header>
    );
}