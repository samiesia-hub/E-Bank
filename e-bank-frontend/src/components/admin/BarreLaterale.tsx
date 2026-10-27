"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { clsx } from "clsx";
import {
    ArrowLeftRight,
    Building2,
    CreditCard,
    FileText,
    KeyRound,
    Landmark,
    BadgeCheck,
    LayoutDashboard,
    LogOut,
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
    { href: "/admin/comptes-bancaires", label: "Comptes", icone: Landmark },
    { href: "/admin/kyc", label: "Vérification KYC", icone: BadgeCheck },
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
        <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-64 shrink-0 flex-col bg-navy md:flex">
            <nav aria-label="Navigation administration" className="flex-1 overflow-y-auto py-space-md">
                <ul className="flex flex-col gap-1">
                    {liens.map(({ href, label, icone: Icone }) => {
                        const actif = estActif(pathname, href);
                        return (
                            <li key={href}>
                                <Link
                                    href={href}
                                    aria-current={actif ? "page" : undefined}
                                    className={clsx(
                                        "flex h-10 items-center gap-3 border-l-[3px] px-space-lg text-body-sm transition-colors",
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

            <div className="border-t border-white/10 p-space-md">
                {/* TODO Partie 7 : appeler l'endpoint de déconnexion du backend */}
                <button
                    type="button"
                    className="flex h-10 w-full items-center gap-3 rounded-lg px-space-md text-body-sm text-white/75 transition-colors hover:bg-white/5 hover:text-white"
                >
                    <LogOut size={18} aria-hidden="true" />
                    <span>Déconnexion</span>
                </button>
            </div>
        </aside>
    );
}