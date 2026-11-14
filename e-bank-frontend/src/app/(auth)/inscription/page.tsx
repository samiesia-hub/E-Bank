"use client";

// Inscription — parcours en 3 étapes.
//   1. Informations  : POST /api/auth/register (cree le compte + ouvre la session)
//   2. Identité      : PUT  /api/client/profile (telephone, adresse)
//   3. Sécurité      : confirmation, puis acces a l'espace client
// La vérification OTP (2FA) reste une maquette : elle n'est pas branchee ici.
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, BadgeCheck, CheckCircle2, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { BASE_API } from "@/lib/client-api";

const etapes = ["Informations", "Identité", "Sécurité"];

export default function PageInscription() {
  const router = useRouter();

  // Parcours
  const [etape, setEtape] = useState(0);
  const [emailCree, setEmailCree] = useState("");

  // Étape 1 — informations
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [afficherMotDePasse, setAfficherMotDePasse] = useState(false);
  const [cguAcceptees, setCguAcceptees] = useState(false);

  // Étape 2 — identité
  const [telephone, setTelephone] = useState("");
  const [adresse, setAdresse] = useState("");

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
          corps?.erreur ?? `Inscription impossible (erreur ${reponse.status}).`
        );
        return;
      }

      // Le cookie de session est posé : les étapes suivantes peuvent appeler
      // les routes /api/client/* sans redemander les identifiants.
      setEmailCree(email);
      setEtape(1);
    } catch {
      setErreur(
        "Le serveur est injoignable. Vérifiez que le backend est démarré, puis réessayez."
      );
    } finally {
      setEnvoi(false);
    }
  }

  /** Étape 2 : enregistre l'identité. `sauter` avance sans rien enregistrer. */
  async function enregistrerIdentite(sauter = false) {
    setErreur(null);

    if (!sauter && telephone.trim() && !/^\d{9,10}$/.test(telephone.trim())) {
      setErreur("Numéro de téléphone invalide (9 ou 10 chiffres).");
      return;
    }

    if (sauter) {
      setEtape(2);
      return;
    }

    setEnvoi(true);
    try {
      const reponse = await fetch(`${BASE_API}/client/profile`, {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          telephone: telephone.trim() || null,
          adresse: adresse.trim() || null,
        }),
      });

      if (!reponse.ok) {
        const corps = await reponse.json().catch(() => null);
        setErreur(
          corps?.message ??
            corps?.erreur ??
            `Enregistrement impossible (erreur ${reponse.status}).`
        );
        return;
      }

      setEtape(2);
    } catch {
      setErreur(
        "Le serveur est injoignable. Vous pouvez passer cette étape et compléter votre profil plus tard."
      );
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

      {/* Étapes du parcours — l'étape en cours est mise en avant. */}
      <ol className="mb-space-lg flex items-center gap-2 rounded-lg bg-surface-container-low p-space-xs">
        {etapes.map((etapeLibelle, index) => {
          const terminee = index < etape;
          const active = index === etape;
          return (
            <li key={etapeLibelle} className="flex flex-1 items-center gap-2">
              <span
                className={
                  active
                    ? "flex h-6 w-6 items-center justify-center rounded-full bg-primary-container font-label-sm text-label-sm text-white"
                    : terminee
                      ? "flex h-6 w-6 items-center justify-center rounded-full bg-tertiary font-label-sm text-label-sm text-white"
                      : "flex h-6 w-6 items-center justify-center rounded-full bg-surface-container font-label-sm text-label-sm text-on-surface-variant"
                }
              >
                {terminee ? <CheckCircle2 className="h-4 w-4" /> : index + 1}
              </span>
              <span
                className={
                  active
                    ? "font-label-sm text-label-sm font-semibold text-on-surface"
                    : "font-label-sm text-label-sm text-on-surface-variant"
                }
              >
                {etapeLibelle}
              </span>
              {index < etapes.length - 1 && (
                <span className="h-px flex-1 bg-surface-container-high" />
              )}
            </li>
          );
        })}
      </ol>

      {erreur && (
        <p
          role="alert"
          className="mb-space-md rounded-lg border border-error-container bg-error-container/40 px-space-sm py-2 font-body-sm text-body-sm text-on-error-container"
        >
          {erreur}
        </p>
      )}

      {/* ── Étape 1 : informations ─────────────────────────────────────────── */}
      {etape === 0 && (
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
      )}

      {/* ── Étape 2 : identité ─────────────────────────────────────────────── */}
      {etape === 1 && (
        <form
          className="space-y-space-md"
          onSubmit={(e) => {
            e.preventDefault();
            void enregistrerIdentite();
          }}
          noValidate
        >
          <p className="rounded-lg bg-surface-container-low px-space-sm py-2 font-body-sm text-body-sm text-on-surface-variant">
            Compte créé pour <span className="font-semibold text-on-surface">{emailCree}</span>.
            Complétez votre identité pour recevoir les codes de validation.
          </p>

          <div className="space-y-space-xs">
            <label
              htmlFor="telephone"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Téléphone
            </label>
            <input
              id="telephone"
              type="tel"
              inputMode="numeric"
              value={telephone}
              onChange={(e) => setTelephone(e.target.value)}
              autoComplete="tel"
              placeholder="0612345678"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
            <p className="font-label-sm text-label-sm text-on-surface-variant">
              9 ou 10 chiffres.
            </p>
          </div>

          <div className="space-y-space-xs">
            <label
              htmlFor="adresse"
              className="block font-label-md text-label-md font-semibold text-on-surface"
            >
              Adresse
            </label>
            <input
              id="adresse"
              value={adresse}
              onChange={(e) => setAdresse(e.target.value)}
              autoComplete="street-address"
              placeholder="12 rue Exemple, Casablanca"
              className="h-11 w-full rounded-lg border border-divider bg-surface-container-lowest px-3.5 font-body-md text-body-md text-on-surface shadow-sm placeholder:text-on-surface-variant/50 focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            />
          </div>

          <button
            type="submit"
            disabled={envoi}
            className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92] active:bg-[#214470] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span>{envoi ? "Enregistrement…" : "Continuer"}</span>
            <ArrowRight className="h-[18px] w-[18px]" />
          </button>

          <button
            type="button"
            onClick={() => void enregistrerIdentite(true)}
            className="w-full text-center font-label-md text-label-md text-on-surface-variant hover:text-on-surface"
          >
            Passer cette étape
          </button>
        </form>
      )}

      {/* ── Étape 3 : sécurité / confirmation ──────────────────────────────── */}
      {etape === 2 && (
        <div className="space-y-space-md">
          <div className="flex items-start gap-space-sm rounded-lg border border-tertiary/30 bg-tertiary/5 p-space-sm">
            <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-tertiary" />
            <div className="space-y-space-xs">
              <p className="font-label-md text-label-md font-semibold text-on-surface">
                Votre compte est actif
              </p>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Vous pouvez vous connecter dès maintenant avec{" "}
                <span className="font-semibold text-on-surface">{emailCree}</span>.
              </p>
            </div>
          </div>

          <div className="rounded-lg bg-surface-container-low p-space-sm">
            <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
              Sécurité
            </p>
            <p className="mt-space-xs font-body-sm text-body-sm text-on-surface-variant">
              Le code à usage unique (OTP) sera demandé pour les opérations
              sensibles, comme les virements. Il se configure depuis votre profil.
            </p>
          </div>

          <button
            type="button"
            onClick={() => {
              router.replace("/tableau-de-bord");
              router.refresh();
            }}
            className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary-container font-label-md text-label-md font-semibold text-white shadow-sm transition-colors hover:bg-[#2C5A92] active:bg-[#214470]"
          >
            <span>Accéder à mon espace</span>
            <ArrowRight className="h-[18px] w-[18px]" />
          </button>
        </div>
      )}
    </div>
  );
}
