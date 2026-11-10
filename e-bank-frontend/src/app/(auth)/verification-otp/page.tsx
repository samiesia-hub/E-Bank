"use client";

// Vérification OTP — étape 2 du parcours : code reçu par e-mail + nouveau mot de passe.
// POST /api/auth/reset-password { email, code, nouveauMotDePasse }.
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2, Eye, EyeOff, KeyRound } from "lucide-react";
import { BASE_API } from "@/lib/client-api";
import {
  CLE_EMAIL_REINIT,
  REGLE_MOT_DE_PASSE,
  motDePasseValide,
} from "@/lib/reinitialisation";

export default function PageVerificationOtp() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [afficher, setAfficher] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [envoi, setEnvoi] = useState(false);
  const [reussi, setReussi] = useState(false);

  useEffect(() => {
    const enregistre = sessionStorage.getItem(CLE_EMAIL_REINIT);
    if (enregistre) setEmail(enregistre);
  }, []);

  async function soumettre(evenement: React.FormEvent) {
    evenement.preventDefault();
    setErreur(null);

    const adresse = email.trim();
    const codeSaisi = code.trim();

    if (!adresse.includes("@")) {
      setErreur("Renseignez l'adresse e-mail du compte.");
      return;
    }
    if (!/^[0-9]{6}$/.test(codeSaisi)) {
      setErreur("Le code comporte 6 chiffres.");
      return;
    }
    if (!motDePasseValide(motDePasse)) {
      setErreur(`Le mot de passe doit contenir ${REGLE_MOT_DE_PASSE}.`);
      return;
    }
    if (motDePasse !== confirmation) {
      setErreur("Les deux mots de passe ne correspondent pas.");
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await fetch(`${BASE_API}/auth/reset-password`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: adresse,
          code: codeSaisi,
          nouveauMotDePasse: motDePasse,
        }),
      });

      if (!reponse.ok) {
        const corps = await reponse.json().catch(() => null);
        setErreur(
          corps?.message ??
            (reponse.status === 400
              ? "Code invalide ou expiré. Demandez un nouveau code."
              : `Réinitialisation impossible (erreur ${reponse.status}).`)
        );
        return;
      }

      sessionStorage.removeItem(CLE_EMAIL_REINIT);
      setReussi(true);
    } catch {
      setErreur(
        "Le service est momentanément indisponible. Vérifiez votre connexion puis réessayez."
      );
    } finally {
      setEnvoi(false);
    }
  }

  if (reussi) {
    return (
      <div className="w-full rounded-lg bg-surface-container-lowest p-space-lg text-center shadow-sm md:p-space-xl">
        <CheckCircle2
          className="mx-auto h-12 w-12 text-tertiary"
          aria-hidden="true"
        />
        <h1 className="mt-space-md font-headline-md text-headline-md font-bold tracking-tight text-on-surface">
          Mot de passe modifié
        </h1>
        <p className="mt-space-sm font-body-sm text-body-sm text-on-surface-variant">
          Vous pouvez maintenant vous connecter avec votre nouveau mot de passe. Le code
          utilisé n&apos;est plus valable.
        </p>
        <button
          type="button"
          onClick={() => router.push("/connexion")}
          className="mt-space-lg flex h-11 w-full items-center justify-center rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92]"
        >
          Se connecter
        </button>
      </div>
    );
  }

  return (
    <div className="w-full rounded-lg bg-surface-container-lowest p-space-lg shadow-sm md:p-space-xl">
      <Link
        href="/mot-de-passe-oublie"
        className="mb-space-lg inline-flex items-center gap-1 font-label-sm text-label-sm font-semibold text-on-surface-variant hover:text-primary"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Renvoyer un code
      </Link>

      <div className="mb-space-lg">
        <h1 className="mb-space-xs font-headline-md text-headline-md font-bold tracking-tight text-on-surface">
          Vérification du code
        </h1>
        <p className="font-body-sm text-body-sm text-on-surface-variant">
          Saisissez le code reçu par e-mail, puis choisissez votre nouveau mot de passe.
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
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="nom@exemple.ma"
            className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
          />
        </div>

        <div className="space-y-space-xs">
          <label
            htmlFor="code"
            className="block font-label-md text-label-md font-semibold text-on-surface"
          >
            Code de réinitialisation
          </label>
          <div className="relative flex items-center">
            <input
              id="code"
              name="code"
              inputMode="numeric"
              autoComplete="one-time-code"
              required
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\s/g, ""))}
              placeholder="123456"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest pl-3.5 pr-11 font-body-md text-body-md tracking-[0.3em] text-on-surface shadow-sm placeholder:tracking-normal placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
            <KeyRound
              className="pointer-events-none absolute right-3.5 h-5 w-5 text-on-surface-variant/60"
              aria-hidden="true"
            />
          </div>
        </div>

        <div className="space-y-space-xs">
          <label
            htmlFor="motDePasse"
            className="block font-label-md text-label-md font-semibold text-on-surface"
          >
            Nouveau mot de passe
          </label>
          <div className="relative flex items-center">
            <input
              id="motDePasse"
              name="motDePasse"
              type={afficher ? "text" : "password"}
              autoComplete="new-password"
              required
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              placeholder="••••••••••••"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest pl-3.5 pr-11 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
            <button
              type="button"
              onClick={() => setAfficher((v) => !v)}
              aria-label={afficher ? "Masquer le mot de passe" : "Afficher le mot de passe"}
              className="absolute right-2.5 rounded p-1 text-on-surface-variant hover:text-on-surface"
            >
              {afficher ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </button>
          </div>
          <p className="font-label-sm text-label-sm text-on-surface-variant">
            {REGLE_MOT_DE_PASSE}.
          </p>
        </div>

        <div className="space-y-space-xs">
          <label
            htmlFor="confirmation"
            className="block font-label-md text-label-md font-semibold text-on-surface"
          >
            Confirmer le mot de passe
          </label>
          <input
            id="confirmation"
            name="confirmation"
            type={afficher ? "text" : "password"}
            autoComplete="new-password"
            required
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            placeholder="••••••••••••"
            className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
          />
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
          {envoi ? "Vérification…" : "Valider et changer le mot de passe"}
        </button>
      </form>

      <p className="mt-space-lg rounded-lg bg-surface-container-low p-space-sm font-body-sm text-body-sm text-on-surface-variant">
        Le code expire rapidement et ne peut servir qu&apos;une seule fois. Sans code
        valable, recommencez depuis « Mot de passe oublié ».
      </p>
    </div>
  );
}
