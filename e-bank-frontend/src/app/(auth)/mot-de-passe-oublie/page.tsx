"use client";

// Mot de passe oublié — étape 1 : saisie de l'e-mail.
// POST /api/auth/forgot-password génère un code OTP ; la réponse est identique
// que l'adresse existe ou non (pas d'énumération de comptes).
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Mail } from "lucide-react";
import { BASE_API } from "@/lib/client-api";
import { CLE_EMAIL_REINIT } from "@/lib/reinitialisation";

export default function PageMotDePasseOublie() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [erreur, setErreur] = useState<string | null>(null);
  const [envoi, setEnvoi] = useState(false);

  async function soumettre(evenement: React.FormEvent) {
    evenement.preventDefault();
    setErreur(null);

    const adresse = email.trim();
    if (!adresse.includes("@") || adresse.length < 5) {
      setErreur("Saisissez une adresse e-mail valide.");
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await fetch(`${BASE_API}/auth/forgot-password`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: adresse }),
      });

      if (!reponse.ok) {
        const corps = await reponse.json().catch(() => null);
        setErreur(
          corps?.message ??
            `Envoi impossible (erreur ${reponse.status}). Réessayez dans un instant.`
        );
        return;
      }

      // L'e-mail sert d'identifiant à l'étape 2 ; on le garde le temps du parcours.
      sessionStorage.setItem(CLE_EMAIL_REINIT, adresse);
      router.push("/verification-otp");
    } catch {
      setErreur(
        "Le service est momentanément indisponible. Vérifiez votre connexion puis réessayez."
      );
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <div className="w-full rounded-lg bg-surface-container-lowest p-space-lg shadow-sm md:p-space-xl">
      <Link
        href="/connexion"
        className="mb-space-lg inline-flex items-center gap-1 font-label-sm text-label-sm font-semibold text-on-surface-variant hover:text-primary"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Retour à la connexion
      </Link>

      <div className="mb-space-lg">
        <h1 className="mb-space-xs font-headline-md text-headline-md font-bold tracking-tight text-on-surface">
          Mot de passe oublié
        </h1>
        <p className="font-body-sm text-body-sm text-on-surface-variant">
          Indiquez l&apos;adresse e-mail de votre compte. Nous vous enverrons un code de
          réinitialisation à usage unique.
        </p>
      </div>

      <form className="space-y-space-md" onSubmit={soumettre} noValidate>
        <div className="space-y-space-xs">
          <label
            htmlFor="email"
            className="block font-label-md text-label-md font-semibold text-on-surface"
          >
            Adresse e-mail
          </label>
          <div className="relative flex items-center">
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="nom@exemple.ma"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest pl-3.5 pr-10 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
            <Mail
              className="pointer-events-none absolute right-3.5 h-5 w-5 text-on-surface-variant/60"
              aria-hidden="true"
            />
          </div>
        </div>

        {erreur && (
          <p
            role="alert"
            className="rounded-lg border border-error-container bg-error-container/40 px-space-sm py-2 font-body-sm text-body-sm text-on-error-container"
          >
            {erreur}
          </p>
        )}

        <button
          type="submit"
          disabled={envoi}
          className="flex h-11 w-full items-center justify-center rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92] active:bg-[#214470] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {envoi ? "Envoi du code…" : "Recevoir un code"}
        </button>
      </form>

      <p className="mt-space-lg rounded-lg bg-surface-container-low p-space-sm font-body-sm text-body-sm text-on-surface-variant">
        Par sécurité, la réponse est la même que l&apos;adresse soit enregistrée ou non.
        Le code expire rapidement et ne peut servir qu&apos;une fois.
      </p>
    </div>
  );
}
