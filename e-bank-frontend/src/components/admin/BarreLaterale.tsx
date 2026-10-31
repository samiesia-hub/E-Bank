"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { clsx } from "clsx";
import {
    ArrowLeftRight,
    BadgeCheck,
    Building2,
    CreditCard,
    FileText,
    KeyRound,
    Landmark,
    LayoutDashboard,
    LogOut,
    Menu,
    ScrollText,
    Settings,
    ShieldAlert,
    UserCog,
    Users,
    type LucideIcon,
} from "lucide-react";

interface LienMenu {
    href: string;
    label: string;
    icone: LucideIcon;
}

// Les pages marquées (*) n'existent pas encore : le lien donnera un 404 pour l'instant.
const liens: LienMenu[] = [
    { href: "/admin", label: "Vue d'ensemble", icone: LayoutDashboard },
    { href: "/admin/utilisateurs", label: "Clients", icone: Users },
    { href: "/admin/kyc", label: "Vérification KYC", icone: BadgeCheck },
    { href: "/admin/comptes-bancaires", label: "Comptes", icone: Landmark },
    { href: "/admin/transactions", label: "Transactions", icone: ArrowLeftRight },
    { href: "/admin/demandes-credit", label: "Demandes de crédit", icone: FileText }, // (*)
    { href: "/admin/cartes", label: "Cartes", icone: CreditCard }, // (*)
    { href: "/admin/alertes-fraude", label: "Alertes fraude", icone: ShieldAlert }, // (*)
    { href: "/admin/agences", label: "Agences", icone: Building2 }, // (*)
    { href: "/admin/employes-support", label: "Employés", icone: UserCog },
    { href: "/admin/roles-permissions", label: "Rôles et permissions", icone: KeyRound },
    { href: "/admin/audit", label: "Journal d'audit", icone: ScrollText },
    { href: "/admin/parametres", label: "Paramètres", icone: Settings }, // (*)
];

// Un lien est "actif" si l'URL courante est ce lien ou l'une de ses sous-pages.
function estActif(pathname: string, href: string): boolean {
    if (href === "/admin") return pathname === "/admin"; // exact, sinon toujours actif
    return pathname === href || pathname.startsWith(href + "/");
}

export function BarreLaterale() {
    const pathname = usePathname();

    return (
        // Ce conteneur réserve 56 px dans la page : le contenu ne bouge jamais.
        <div className="relative hidden w-14 shrink-0 md:block">
            <aside
                aria-label="Menu d'administration"
                className="group sticky top-20 z-30 flex h-[calc(100vh-5rem)] w-14 flex-col overflow-hidden bg-navy transition-[width] duration-200 ease-out focus-within:w-64 hover:w-64"
            >
                {/* Les "3 lignes" : seul élément visible quand la barre est réduite */}
                <button
                    type="button"
                    aria-label="Menu"
                    className="flex h-12 w-14 shrink-0 items-center justify-center text-white"
                >
                    <Menu size={24} aria-hidden="true" />
                </button>

                {/* Contenu complet : invisible tant que la barre est réduite.
            Largeur fixe (w-64) pour que les textes ne se déplacent pas pendant l'animation. */}
                <div className="invisible flex min-h-0 w-64 flex-1 flex-col opacity-0 transition-opacity duration-150 group-focus-within:visible group-focus-within:opacity-100 group-hover:visible group-hover:opacity-100">
                    <div className="defilement-sombre min-h-0 flex-1 overflow-y-auto pb-space-sm">
                        <nav aria-label="Navigation administration">
                            <ul className="flex flex-col gap-0.5">
                                {liens.map(({ href, label, icone: Icone }) => {
                                    const actif = estActif(pathname, href);
                                    return (
                                        <li key={href}>
                                            <Link
                                                href={href}
                                                aria-current={actif ? "page" : undefined}
                                                className={clsx(
                                                    "flex h-9 items-center gap-3 border-l-[3px] px-space-lg text-body-sm transition-colors",
                                                    actif
                                                        ? "border-primary-container bg-white/10 font-semibold text-white"
                                                        : "border-transparent text-white/75 hover:bg-white/5 hover:text-white"
                                                )}
                                            >
                                                <Icone size={18} aria-hidden="true" />
                                                <span>{label}</span>
                                            </Link>
                                        </li>
                                    );
                                })}
                            </ul>
                        </nav>

                        {/* Profil, juste sous "Paramètres".
                TODO Partie 7 : nom et rôle viennent de la session, pas du texte fixe */}
                        <div className="mx-space-md mt-space-md flex items-center gap-3 border-t border-white/10 pt-space-md">
                            <div
                                aria-hidden="true"
                                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-container text-label-md font-semibold text-white"
                            >
                                AB
                            </div>
                            <div className="flex min-w-0 flex-col">
                <span className="text-label-md font-semibold text-white">
                  Admin Ora Bank
                </span>
                                <span className="text-label-sm text-white/60">
                  Administrateur
                </span>
                            </div>
                        </div>
                    </div>

                    <div className="shrink-0 border-t border-white/10 p-space-sm">
                        {/* TODO Partie 7 : appeler l'endpoint de déconnexion du backend */}
                        <button
                            type="button"
                            className="flex h-9 w-full items-center gap-3 rounded-lg px-space-md text-body-sm text-white/75 transition-colors hover:bg-white/5 hover:text-white"
                        >
                            <LogOut size={18} aria-hidden="true" />
                            <span>Déconnexion</span>
                        </button>
                    </div>
                </div>
            </aside>
        </div>
    );
}