"use client";

// Connexion — mockup designs/..._homepage (4)/code.html
// Identifiants → POST /api/auth/login, le backend pose le cookie httpOnly "session".
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { BadgeCheck, Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";
import { BASE_API } from "@/lib/client-api";

// Mode démonstration : ce compte fonctionne même sans backend.
const COMPTE_DEMO = { email: "demo@bankati.ma", motDePasse: "Bankati2026!" };
const CLE_COMPTES = "bankati.comptes-demo";

function trouverCompteDemo(email: string, motDePasse: string) {
  const enregistres = JSON.parse(localStorage.getItem(CLE_COMPTES) ?? "[]");
  const comptes = [COMPTE_DEMO, ...enregistres] as {
    email: string;
    motDePasse: string;
  }[];
  return comptes.find(
    (c) => c.email.toLowerCase() === email.toLowerCase() && c.motDePasse === motDePasse
  );
}

export default function PageConnexion() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [afficherMotDePasse, setAfficherMotDePasse] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [envoi, setEnvoi] = useState(false);

  async function soumettre(evenement: React.FormEvent) {
    evenement.preventDefault();
    setErreur(null);

    if (!email.includes("@")) {
      setErreur("Saisissez une adresse e-mail valide.");
      return;
    }
    if (!motDePasse) {
      setErreur("Le mot de passe est obligatoire.");
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await fetch(`${BASE_API}/auth/login`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, motDePasse }),
      });

      if (!reponse.ok) {
        const corps = await reponse.json().catch(() => null);
        setErreur(
          corps?.erreur ??
            (reponse.status === 401
              ? "Email ou mot de passe incorrect."
              : `Connexion impossible (erreur ${reponse.status}).`)
        );
        return;
      }

      router.replace("/");
      router.refresh();
    } catch {
      // Backend éteint : on accepte les comptes de démonstration.
      if (trouverCompteDemo(email, motDePasse)) {
        router.replace("/");
        router.refresh();
        return;
      }
      setErreur("Email ou mot de passe incorrect.");
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <>
      <div className="w-full rounded-lg bg-surface-container-lowest p-space-lg shadow-sm md:p-space-xl">
        <div className="mb-space-lg">
          <h1 className="mb-space-xs font-headline-md text-headline-md font-bold tracking-tight text-on-surface">
            Connexion à votre espace
          </h1>
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Accédez à vos comptes Bankati
          </p>
        </div>

        <form className="space-y-space-md" onSubmit={soumettre} noValidate>
          <div className="space-y-space-xs">
            <label
              htmlFor="email"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Identifiant ou e-mail
            </label>
            <div className="relative flex items-center">
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Ex : 24891024 ou nom@exemple.ma"
                className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest pl-3.5 pr-10 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
              />
              <BadgeCheck className="pointer-events-none absolute right-3.5 h-5 w-5 text-on-surface-variant/60" />
            </div>
          </div>

          <div className="space-y-space-xs">
            <label
              htmlFor="motDePasse"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Mot de passe
            </label>
            <div className="relative flex items-center">
              <input
                id="motDePasse"
                name="motDePasse"
                type={afficherMotDePasse ? "text" : "password"}
                autoComplete="current-password"
                required
                value={motDePasse}
                onChange={(e) => setMotDePasse(e.target.value)}
                placeholder="••••••••••••"
                className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest pl-3.5 pr-11 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
              />
              <button
                type="button"
                onClick={() => setAfficherMotDePasse((v) => !v)}
                aria-label={
                  afficherMotDePasse
                    ? "Masquer le mot de passe"
                    : "Afficher le mot de passe"
                }
                className="absolute right-2.5 rounded p-1 text-on-surface-variant hover:text-on-surface"
              >
                {afficherMotDePasse ? (
                  <EyeOff className="h-5 w-5" />
                ) : (
                  <Eye className="h-5 w-5" />
                )}
              </button>
            </div>
          </div>

          <div className="flex justify-end pt-1">
            <Link
              href="/mot-de-passe-oublie"
              className="font-label-md text-label-md font-semibold text-primary hover:underline"
            >
              Mot de passe oublié ?
            </Link>
          </div>

          {erreur && (
            <p
              role="alert"
              className="rounded-lg border border-error-container bg-error-container/40 px-space-sm py-2 font-body-sm text-body-sm text-on-error-container"
            >
              {erreur}
            </p>
          )}

          <div className="pt-space-xs">
            <button
              type="submit"
              disabled={envoi}
              className="flex h-11 w-full items-center justify-center gap-space-xs rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92] active:bg-[#214470] disabled:cursor-not-allowed disabled:opacity-60"
            >
              <span>{envoi ? "Connexion…" : "Se connecter"}</span>
              <LockKeyhole className="h-[18px] w-[18px]" />
            </button>
          </div>
        </form>

        <div className="relative my-space-lg flex items-center justify-center">
          <div className="h-px w-full bg-surface-container-high" />
          <span className="absolute bg-surface-container-lowest px-space-md font-label-sm text-label-sm font-semibold uppercase tracking-widest text-on-surface-variant/70">
            OU
          </span>
        </div>

        <p className="text-center font-body-sm text-body-sm text-on-surface-variant">
          Pas encore client ?
          <Link
            href="/inscription"
            className="ml-1 font-semibold text-primary hover:underline"
          >
            Créer un compte
          </Link>
        </p>

        <div className="mt-space-lg flex items-center justify-center gap-2 rounded-lg bg-surface-container-low p-space-sm text-center">
          <ShieldCheck className="h-[18px] w-[18px] text-tertiary" />
          <span className="font-label-sm text-label-sm text-on-surface-variant">
            Connexion chiffrée SSL 256 bits et protection anti-fraude active
          </span>
        </div>
      </div>

      <div className="mt-space-md flex items-center justify-between px-space-sm text-on-surface-variant/70">
        <div className="flex items-center gap-space-sm">
          <span className="font-label-sm text-label-sm">Assistance 24/7</span>
          <span className="text-surface-dim">•</span>
          <span className="font-label-sm text-label-sm">Sécurité</span>
        </div>
        <div className="flex items-center gap-1 font-label-sm text-label-sm">
          <ShieldCheck className="h-3.5 w-3.5 text-tertiary" />
          <span>Serveurs conformes PCI-DSS</span>
        </div>
      </div>
    </>
  );
}
