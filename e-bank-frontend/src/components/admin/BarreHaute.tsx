import Image from "next/image";
import { Bell } from "lucide-react";
import { RechercheGlobale } from "@/components/admin/RechercheGlobale";

export function BarreHaute() {
    return (
        <header className="sticky top-0 z-20 flex h-20 items-center justify-between bg-navy text-white">
            {/* Logo : le billet à gauche, « ORA BANK » et « DEPUIS 2026 » à droite */}
            <div
                className="flex shrink-0 items-center px-space-lg"
                style={{ gap: "24px" }}
            >
                <Image
                    src="/logo-ora-bank-icone.png"
                    alt=""
                    width={289}
                    height={153}
                    priority
                    className="h-10 w-auto"
                />
                <div
                    className="flex flex-col items-center text-ora-gold"
                    style={{ lineHeight: 1 }}
                >
        <span
            style={{
                fontSize: "16px",
                fontWeight: 600,
                letterSpacing: "0.2em",
                marginRight: "-0.2em",
                textTransform: "uppercase",
            }}
        >
            Ora Bank
        </span>
                    <span
                        style={{
                            marginTop: "8px",
                            fontSize: "8px",
                            fontWeight: 400,
                            letterSpacing: "0.38em",
                            marginRight: "-0.38em",
                            textTransform: "uppercase",
                        }}
                    >
            Depuis 2026
        </span>
                </div>
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