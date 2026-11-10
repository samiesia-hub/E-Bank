import Link from "next/link";
import {
  AlertTriangle,
  ClipboardCheck,
  FileCheck,
  KeyRound,
  Lock,
  ShieldCheck,
  Smartphone,
} from "lucide-react";

// Centre sécurité — reprend uniquement les protections réellement en place
// (OTP, KYC, journal d'audit, session invalidée à la déconnexion, opposition carte).

const conteneur = "mx-auto w-full max-w-[1200px] px-margin";

const protections = [
  {
    icone: Smartphone,
    titre: "Authentification forte (OTP)",
    texte:
      "La connexion et chaque virement sont confirmés par un code à usage unique envoyé sur votre e-mail. Le code expire rapidement et ne peut servir qu'une fois.",
  },
  {
    icone: Lock,
    titre: "Chiffrement des échanges",
    texte:
      "Les échanges entre votre navigateur et la banque sont chiffrés en SSL 256 bits. Votre session repose sur un cookie protégé, inaccessible au code des pages.",
  },
  {
    icone: FileCheck,
    titre: "Vérification d'identité (KYC)",
    texte:
      "L'ouverture d'un compte exige une pièce d'identité vérifiée par l'équipe. Un document refusé ne peut pas être remplacé sans motif affiché.",
  },
  {
    icone: ClipboardCheck,
    titre: "Journal d'audit",
    texte:
      "Chaque opération sensible est enregistrée : action, auteur et détails. Le journal permet de reconstituer un historique en cas de contestation.",
  },
  {
    icone: KeyRound,
    titre: "Déconnexion réelle",
    texte:
      "Se déconnecter invalide la session côté serveur : le cookie de connexion ne peut plus être réutilisé, même recopié ailleurs.",
  },
  {
    icone: ShieldCheck,
    titre: "Opposition immédiate",
    texte:
      "En cas de perte ou de vol de carte, l'opposition se demande depuis votre espace ou depuis la page de connexion, à toute heure.",
  },
];

const bonnesPratiques = [
  "Choisissez un mot de passe long et unique : 12 caractères minimum, avec majuscule, minuscule et chiffre.",
  "Ne communiquez jamais un code reçu par e-mail : la banque ne le demande jamais par téléphone.",
  "Vérifiez l'adresse affichée dans le navigateur avant de saisir vos identifiants.",
  "Utilisez un appareil à jour et évitez les réseaux Wi-Fi publics pour vos opérations.",
  "Relisez l'historique de vos virements : toute opération inconnue doit être signalée sans attendre.",
];

export const metadata = {
  title: "Centre sécurité — Ora Bank",
  description:
    "Authentification forte, chiffrement, vérification d'identité et journal d'audit : comment Ora Bank protège vos comptes.",
};

export default function PageSecurite() {
  return (
    <main>
      <section aria-label="Sécurité" className="border-b border-divider bg-surface-tint">
        <div className={`${conteneur} py-space-2xl`}>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
            Sécurité
          </p>
          <h1 className="mt-space-md max-w-2xl text-balance font-display-hero text-display-hero-mobile text-navy lg:text-display-hero">
            Vos comptes protégés à chaque étape.
          </h1>
          <p className="mt-space-md max-w-xl font-body-lg text-body-lg text-neutral-core">
            Authentification forte, chiffrement, vérification d&apos;identité et traçabilité
            des opérations : voici concrètement ce qui protège votre argent.
          </p>
        </div>
      </section>

      <section aria-label="Protections en place" className="border-b border-divider">
        <div className={`${conteneur} py-space-2xl`}>
          <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy lg:text-headline-section">
            Ce qui vous protège
          </h2>
          <ul className="mt-space-xl grid gap-gutter sm:grid-cols-2 lg:grid-cols-3">
            {protections.map((protection) => (
              <li key={protection.titre} className="border-t border-navy pt-space-md">
                <protection.icone
                  size={24}
                  className="text-primary-container"
                  aria-hidden="true"
                />
                <h3 className="mt-space-sm font-label-md text-label-md font-semibold text-navy">
                  {protection.titre}
                </h3>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                  {protection.texte}
                </p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section
        aria-label="Bonnes pratiques"
        className="border-b border-divider bg-surface-container-low"
      >
        <div className={`${conteneur} grid gap-space-xl py-space-2xl lg:grid-cols-[1fr_1.2fr]`}>
          <div>
            <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy lg:text-headline-section">
              Les réflexes qui comptent
            </h2>
            <p className="mt-space-md max-w-md font-body-md text-body-md text-neutral-core">
              La sécurité se joue autant côté banque que côté client. Ces cinq règles
              couvrent l&apos;essentiel des tentatives de fraude.
            </p>
          </div>
          <ol className="space-y-space-md border-l border-divider pl-space-lg">
            {bonnesPratiques.map((pratique, index) => (
              <li key={pratique} className="flex gap-space-md">
                <span className="font-label-sm text-label-sm font-semibold text-access-teal">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <p className="font-body-md text-body-md text-navy">{pratique}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section aria-label="En cas de problème">
        <div className={`${conteneur} grid gap-space-xl py-space-2xl lg:grid-cols-2`}>
          <div className="flex items-start gap-space-md rounded border border-divider p-space-lg">
            <AlertTriangle
              size={22}
              className="mt-1 shrink-0 text-error"
              aria-hidden="true"
            />
            <div>
              <h2 className="font-label-md text-label-md font-semibold text-navy">
                Carte perdue ou volée
              </h2>
              <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                Demandez l&apos;opposition immédiate depuis votre espace client. Si vous
                n&apos;arrivez pas à vous connecter, l&apos;option reste accessible depuis
                la page de connexion.
              </p>
              <Link
                href="/connexion"
                className="mt-space-sm inline-block font-label-md text-label-md font-semibold text-primary hover:underline"
              >
                Demander une opposition
              </Link>
            </div>
          </div>

          <div className="flex items-start gap-space-md rounded border border-divider p-space-lg">
            <ClipboardCheck
              size={22}
              className="mt-1 shrink-0 text-primary-container"
              aria-hidden="true"
            />
            <div>
              <h2 className="font-label-md text-label-md font-semibold text-navy">
                Opération que vous ne reconnaissez pas
              </h2>
              <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                Ouvrez une réclamation depuis votre espace : elle est horodatée, suivie par
                un agent et conservée avec l&apos;historique du compte. Le journal
                d&apos;audit permet de retrouver l&apos;origine de l&apos;opération.
              </p>
              <Link
                href="/connexion"
                className="mt-space-sm inline-block font-label-md text-label-md font-semibold text-primary hover:underline"
              >
                Déposer une réclamation
              </Link>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
