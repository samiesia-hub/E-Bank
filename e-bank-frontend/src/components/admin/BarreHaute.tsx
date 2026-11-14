import Image from "next/image";
import { Bell } from "lucide-react";
import { RechercheGlobale } from "@/components/admin/RechercheGlobale";

export function BarreHaute() {
    return (
        <header className="sticky top-0 z-20 flex h-20 items-center justify-between bg-navy text-white">
            {/* Logo Ora Bank */}
            <div className="flex shrink-0 items-center px-space-lg">
                <Image
                    src="/logo-ora-bank-clair.png"
                    alt="Ora Bank"
                    width={525}
                    height={317}
                    priority
                    className="h-14 w-auto"
                />
            </div>
            {/* Recherche et notifications : à l'extrémité droite de la barre */}
            <div className="flex items-center gap-1 px-space-lg">
                <RechercheGlobale />
                <button
                    type="button"
                    aria-label="Notifications"
                    className="relative flex h-10 w-10 items-center justify-center rounded-lg transition-colors hover:bg-white/10"
                >
                    <Bell size={20} aria-hidden="true" />
                    <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-accent-yellow" />
                </button>
            </div>
        </header>
    );
}