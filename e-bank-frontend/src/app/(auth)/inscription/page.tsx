"use client";

// Inscription — mockup designs/..._homepage (6)/code.html
// Étape 1 (informations) : crée le compte via POST /api/auth/register.
// Les étapes 2 (identité/KYC) et 3 (sécurité/OTP) restent à brancher.
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, BadgeCheck, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { BASE_API } from "@/lib/client-api";

const etapes = ["Informations", "Identité", "Sécurité"];

export default function PageInscription() {
  const router = useRouter();
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [afficherMotDePasse, setAfficherMotDePasse] = useState(false);
  const [cguAcceptees, setCguAcceptees] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [envoi, setEnvoi] = useState(false);

  const criteres = [
    motDePasse.length >= 8,
    /[A-Z]/.test(motDePasse),
    /\d/.test(motDePasse),
    /[^A-Za-z0-9]/.test(motDePasse),
  ];
  const score = criteres.filter(Boolean).length;
  const niveau = ["Faible", "Faible", "Moyen", "Bon", "Fort"][score];

  async function soumettre(evenement: React.FormEvent) {
    evenement.preventDefault();
    setErreur(null);

    if (nom.trim().length < 2 || prenom.trim().length < 2) {
      setErreur("Nom et prénom sont obligatoires.");
      return;
    }
    if (!email.includes("@")) {
      setErreur("Saisissez une adresse e-mail valide.");
      return;
    }
    if (motDePasse.length < 8) {
      setErreur("Le mot de passe doit contenir au moins 8 caractères.");
      return;
    }
    if (motDePasse !== confirmation) {
      setErreur("Les mots de passe ne correspondent pas.");
      return;
    }
    if (!cguAcceptees) {
      setErreur("Acceptez les conditions générales pour continuer.");
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await fetch(`${BASE_API}/auth/register`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          motDePasse,
          nom: `${prenom.trim()} ${nom.trim()}`,
        }),
      });

      if (!reponse.ok) {
        const corps = await reponse.json().catch(() => null);
        setErreur(
          corps?.erreur ??
            `Inscription impossible (erreur ${reponse.status}).`
        );
        return;
      }

      router.replace("/");
      router.refresh();
    } catch {
      // Backend éteint : le compte est gardé dans le navigateur (mode démonstration).
      const CLE_COMPTES = "bankati.comptes-demo";
      const enregistres = JSON.parse(localStorage.getItem(CLE_COMPTES) ?? "[]");
      localStorage.setItem(
        CLE_COMPTES,
        JSON.stringify([...enregistres, { email, motDePasse }])
      );
      router.replace("/");
      router.refresh();
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <div className="w-full rounded-lg bg-surface-container-lowest p-space-lg shadow-sm md:p-space-xl">
      <div className="mb-space-md flex items-start justify-between gap-3">
        <div>
          <span className="font-label-sm text-label-sm font-semibold uppercase tracking-wider text-on-surface-variant">
            Espace particuliers
          </span>
          <h1 className="mt-space-xs font-headline-md text-headline-md font-bold tracking-tight text-on-surface">
            Créez votre compte
          </h1>
          <p className="font-body-sm text-body-sm text-on-surface-variant">
            Ouvrez votre compte bancaire en ligne en 5 minutes
          </p>
        </div>
        <span className="inline-flex shrink-0 items-center gap-1 rounded border border-tertiary/30 px-2 py-0.5 text-tertiary">
          <ShieldCheck className="h-3.5 w-3.5" />
          <span className="font-label-sm text-label-sm">Portail Agréé</span>
        </span>
      </div>

      {/* Étapes du parcours — seule l'étape 1 est active pour l'instant. */}
      <ol className="mb-space-lg flex items-center gap-2 rounded-lg bg-surface-container-low p-space-xs">
        {etapes.map((etape, index) => (
          <li key={etape} className="flex flex-1 items-center gap-2">
            <span
              className={
                index === 0
                  ? "flex h-6 w-6 items-center justify-center rounded-full bg-primary-container font-label-sm text-label-sm text-white"
                  : "flex h-6 w-6 items-center justify-center rounded-full bg-surface-container font-label-sm text-label-sm text-on-surface-variant"
              }
            >
              {index + 1}
            </span>
            <span
              className={
                index === 0
                  ? "font-label-sm text-label-sm font-semibold text-on-surface"
                  : "font-label-sm text-label-sm text-on-surface-variant"
              }
            >
              {etape}
            </span>
            {index < etapes.length - 1 && (
              <span className="h-px flex-1 bg-surface-container-high" />
            )}
          </li>
        ))}
      </ol>

      <form className="space-y-space-md" onSubmit={soumettre} noValidate>
        <div className="grid gap-space-md sm:grid-cols-2">
          <div className="space-y-space-xs">
            <label
              htmlFor="nom"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Nom
            </label>
            <input
              id="nom"
              value={nom}
              onChange={(e) => setNom(e.target.value)}
              autoComplete="family-name"
              placeholder="Benali"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
          </div>
          <div className="space-y-space-xs">
            <label
              htmlFor="prenom"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Prénom
            </label>
            <input
              id="prenom"
              value={prenom}
              onChange={(e) => setPrenom(e.target.value)}
              autoComplete="given-name"
              placeholder="Karim"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
          </div>
        </div>

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
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              placeholder="karim.benali@exemple.ma"
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
              type={afficherMotDePasse ? "text" : "password"}
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              autoComplete="new-password"
              placeholder="8 caractères minimum"
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
          <div className="flex gap-1 pt-1">
            {criteres.map((ok, index) => (
              <span
                key={index}
                className={
                  ok
                    ? "h-1 flex-1 rounded-full bg-tertiary"
                    : "h-1 flex-1 rounded-full bg-surface-container-high"
                }
              />
            ))}
          </div>
          <div className="flex justify-between font-label-sm text-label-sm text-on-surface-variant">
            <span>Niveau : {motDePasse ? niveau : "—"}</span>
            <span>Majuscule, chiffre, symbole</span>
          </div>
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
            type={afficherMotDePasse ? "text" : "password"}
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            autoComplete="new-password"
            placeholder="••••••••••••"
            className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
          />
        </div>

        <label className="flex cursor-pointer items-start gap-2">
          <input
            type="checkbox"
            checked={cguAcceptees}
            onChange={(e) => setCguAcceptees(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-divider text-primary-container focus:ring-1 focus:ring-primary-container"
          />
          <span className="font-body-sm text-body-sm text-on-surface-variant">
            J&apos;accepte les{" "}
            <span className="font-semibold text-primary">
              conditions générales d&apos;utilisation
            </span>{" "}
            et la politique de protection des données.
          </span>
        </label>

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
          className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92] active:bg-[#214470] disabled:cursor-not-allowed disabled:opacity-60"
        >
          <span>{envoi ? "Création…" : "Continuer"}</span>
          <ArrowRight className="h-[18px] w-[18px]" />
        </button>

        <p className="text-center font-body-sm text-body-sm text-on-surface-variant">
          Déjà client ?
          <Link
            href="/connexion"
            className="ml-1 font-semibold text-primary hover:underline"
          >
            Se connecter
          </Link>
        </p>
      </form>
    </div>
  );
}
