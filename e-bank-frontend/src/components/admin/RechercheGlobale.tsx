"use client";

import { useState } from "react";
import { Search, X } from "lucide-react";

export function RechercheGlobale() {
    // ouvert = true : le champ de recherche est visible
    const [ouvert, setOuvert] = useState(false);

    return (
        <div className="flex items-center gap-1">
            {ouvert && (
                <input
                    type="search"
                    autoFocus
                    aria-label="Recherche globale"
                    placeholder="Rechercher un client, un compte, une transaction"
                    onKeyDown={(e) => {
                        if (e.key === "Escape") setOuvert(false);
                    }}
                    className="h-10 w-96 rounded-lg border border-white/20 bg-white/10 px-3 text-body-sm text-white placeholder:text-white/60 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.4)]"
                />
            )}
            <button
                type="button"
                aria-label={ouvert ? "Fermer la recherche" : "Ouvrir la recherche"}
                aria-expanded={ouvert}
                onClick={() => setOuvert(!ouvert)}
                className="flex h-10 w-10 items-center justify-center rounded-lg transition-colors hover:bg-white/10"
            >
                {ouvert ? (
                    <X size={20} aria-hidden="true" />
                ) : (
                    <Search size={20} aria-hidden="true" />
                )}
            </button>
        </div>
    );
}