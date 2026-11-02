import Link from "next/link";
import {
  Landmark,
  ShieldCheck,
  Lock,
  Smartphone,
  UserPlus,
  Zap,
  PieChart,
  MessagesSquare,
  FileCheck,
  ClipboardCheck,
  CreditCard,
  Check,
} from "lucide-react";

// Page d'accueil publique — design "Sovereign Retail Banking" (DESIGN.md).
// Ton institutionnel, sobre : structure claire, hairlines, aucune ombre lourde.
// Contenus réutilisés de l'app (FGDR, SSL 256 bits, PCI-DSS, OTP/3D Secure) — rien d'inventé.

/* ---------------- Données éditables (les textes se changent ici) ---------------- */

const pointsDeConfiance = [
  { icon: ShieldCheck, titre: "Garantie des dépôts FGDR", texte: "Conformément aux réglementations de l'UE." },
  { icon: Lock, titre: "Chiffrement SSL 256 bits", texte: "Connexion chiffrée de bout en bout." },
  { icon: Landmark, titre: "Conforme PCI-DSS", texte: "Serveurs conformes pour les paiements." },
  { icon: Smartphone, titre: "Authentification forte", texte: "OTP à chaque opération sensible (3D Secure)." },
];

const offres = [
  {
    nom: "Compte Essentiel",
    prix: "Gratuit",
    prixDetail: "0 € de frais de tenue de compte",
    fonctionnalites: [
      "Compte courant en euros",
      "Virements SEPA",
      "Relevés de compte PDF",
      "Support en ligne",
    ],
    cta: "Ouvrir ce compte",
    vitrine: true,
  },
  {
    nom: "Compte Premium",
    prix: "3,99 € / mois",
    prixDetail: "Services étendus et cartes",
    fonctionnalites: [
      "Tous les services du Compte Essentiel",
      "Carte bancaire",
      "Catégories de dépenses",
      "Notifications d'opérations",
      "Support prioritaire",
    ],
    cta: "Ouvrir ce compte",
    vitrine: true,
  },
  {
    nom: "Compte Professionnel",
    prix: "Sur devis",
    prixDetail: "Pour indépendants et petites structures",
    fonctionnalites: [
      "Compte dédié à votre activité",
      "Virements SEPA illimités",
      "Suivi des flux",
      "Export comptable",
    ],
    cta: "Demander une ouverture",
    vitrine: true,
  },
];

const argumentsPoursuivre = [
  {
    icon: UserPlus,
    titre: "Ouverture 100 % en ligne",
    texte: "Inscription, vérification d'identité (KYC) et activation depuis votre navigateur.",
  },
  {
    icon: Zap,
    titre: "Virements SEPA",
    texte: "Ajoutez vos bénéficiaires et envoyez vos virements avec validation OTP.",
  },
  {
    icon: PieChart,
    titre: "Suivi de vos dépenses",
    texte: "Historique, soldes et relevés clairs, téléchargeables en PDF.",
  },
  {
    icon: MessagesSquare,
    titre: "Support humain",
    texte: "Tickets et réclamations suivis par une équipe, pas par un robot.",
  },
];

const mesuresSecurite = [
  { icon: Smartphone, titre: "OTP", texte: "Un code à usage unique pour la connexion et chaque virement." },
  { icon: FileCheck, titre: "KYC", texte: "Vérification de votre identité à l'ouverture du compte." },
  { icon: ClipboardCheck, titre: "Audit", texte: "Journal complet des opérations, consultable à tout moment." },
  { icon: CreditCard, titre: "Opposition carte", texte: "Blocage immédiat de votre carte depuis votre espace." },
];

const chiffres = [
  { valeur: "0 €", libelle: "frais de tenue de compte" },
  { valeur: "24/7", libelle: "accès à vos services" },
  { valeur: "100 %", libelle: "en ligne" },
  { valeur: "256 bits", libelle: "chiffrement SSL" },
];

const temoignages = [
  {
    initiales: "S. L.",
    prenom: "Sophie",
    ville: "Nantes",
    citation:
      "L'ouverture s'est faite en ligne sans difficulté, et les virements sont simples à suivre du début à la fin.",
  },
  {
    initiales: "K. B.",
    prenom: "Karim",
    ville: "Lyon",
    citation:
      "Les relevés sont clairs et le support a répondu à ma réclamation rapidement, avec une vraie personne.",
  },
  {
    initiales: "M. T.",
    prenom: "Mireille",
    ville: "Bordeaux",
    citation:
      "L'authentification par code à chaque virement demande une seconde, mais elle donne un vrai sentiment de sécurité.",
  },
];

const faq = [
  {
    question: "Comment ouvrir un compte Ora Bank ?",
    reponse:
      "Cliquez sur « Ouvrir un compte », complétez le formulaire d'inscription, puis vérifiez le code OTP envoyé sur votre téléphone. Une vérification de votre identité (KYC) est ensuite réalisée avant l'activation du compte.",
  },
  {
    question: "Combien coûte la tenue de compte ?",
    reponse:
      "Le Compte Essentiel est gratuit, sans frais de tenue de compte. Les offres Premium et Professionnel détaillent leurs tarifs dans la section Comptes ci-dessus.",
  },
  {
    question: "Comment fonctionnent les virements SEPA ?",
    reponse:
      "Depuis votre espace client, ajoutez un bénéficiaire puis suivez l'assistant de virement en quatre étapes. Chaque virement est confirmé par un code OTP et un reçu vous est délivré.",
  },
  {
    question: "Ma connexion est-elle sécurisée ?",
    reponse:
      "Oui. Les échanges sont chiffrés en SSL 256 bits, la connexion exige une authentification forte (OTP) et chaque opération sensible est journalisée pour audit.",
  },
  {
    question: "Comment faire opposition à ma carte ?",
    reponse:
      "Connectez-vous à votre espace client et demandez l'opposition immédiate. En cas de doute, l'option reste accessible depuis la page de connexion.",
  },
];

/* Classe utilitaire commune : conteneur 1200px + marges */
const conteneur = "mx-auto w-full max-w-[1200px] px-margin";

export default function Home() {
  return (
    <main>
      {/* 1. HERO — promesse concrète + visuel produit CSS (aucune image externe) */}
      <section aria-label="Présentation" className="border-b border-divider bg-surface-tint">
        <div className={`${conteneur} grid items-center gap-space-xl py-space-2xl lg:grid-cols-2`}>
          <div>
            <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
              Ora Bank — Banque en ligne
            </p>
            <h1 className="mt-space-md max-w-xl text-balance font-display-hero text-display-hero-mobile text-navy lg:text-display-hero">
              Votre compte, votre IBAN et vos virements SEPA — 100 % en ligne.
            </h1>
            <p className="mt-space-md max-w-xl font-body-lg text-body-lg text-neutral-core">
              Gérez vos soldes, vos virements et vos relevés à tout moment, protégé par une
              authentification forte (OTP) et une vérification d&apos;identité complète (KYC).
            </p>
            <div className="mt-space-xl flex flex-col gap-space-md sm:flex-row">
              <Link
                href="/inscription"
                className="inline-flex h-10 w-full items-center justify-center rounded-lg bg-primary-container px-space-lg font-label-md text-label-md text-white hover:bg-[#2C5A92] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-container sm:w-auto"
              >
                Ouvrir un compte
              </Link>
              <Link
                href="/connexion"
                className="inline-flex h-10 w-full items-center justify-center rounded-lg bg-access-teal px-space-lg font-label-md text-label-md text-white hover:bg-[#307272] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-access-teal sm:w-auto"
              >
                Accès client
              </Link>
            </div>
            <p className="mt-space-lg inline-flex items-center gap-space-xs font-body-sm text-body-sm text-neutral-core">
              <ShieldCheck size={16} className="text-access-teal" aria-hidden="true" />
              Vos avoirs couverts par le FGDR, conformément aux réglementations de l&apos;UE.
            </p>
          </div>

          {/* Visuel produit : compte + carte (données fictives, présentées comme telles) */}
          <div className="hidden max-w-sm justify-self-center lg:block" aria-hidden="true">
            <div className="rounded-lg border border-divider bg-white p-space-lg">
              <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
                Compte courant
              </p>
              <p className="tabular mt-space-sm font-numeric-data text-numeric-data text-navy">
                4 258,60 €
              </p>
              <p className="tabular mt-space-md font-label-md text-label-md text-neutral-core">
                FR76 •••• •••• 1234
              </p>
              <div className="mt-space-md grid grid-cols-3 gap-space-md border-t border-divider pt-space-md text-center">
                <div>
                  <p className="tabular font-body-sm text-body-sm font-semibold text-navy">1 240,00 €</p>
                  <p className="font-label-sm text-label-sm text-neutral-core">Dernier virement</p>
                </div>
                <div>
                  <p className="tabular font-body-sm text-body-sm font-semibold text-navy">−36,50 €</p>
                  <p className="font-label-sm text-label-sm text-neutral-core">Dépenses du jour</p>
                </div>
                <div>
                  <p className="font-body-sm text-body-sm font-semibold text-access-teal">Validé</p>
                  <p className="font-label-sm text-label-sm text-neutral-core">Statut compte</p>
                </div>
              </div>
              <div className="mt-space-lg flex items-end justify-between">
                <div className="h-16 w-28 rounded-lg bg-navy p-2 text-white">
                  <div className="mt-1 h-2 w-2.5 rounded-sm bg-[#8DB8E8]" />
                  <p className="mt-2 font-label-md text-label-md tabular">•••• 1234</p>
                  <p className="font-label-sm text-label-sm text-white/60">Titulaire du compte</p>
                </div>
                <p className="font-label-sm text-label-sm text-neutral-core">
                  Données fictives — exemple d&apos;affichage
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. BANDE DE CONFIANCE — réutilise uniquement des éléments affichés dans l'app */}
      <section aria-label="Confiance et conformité" className="border-b border-divider">
        <ul className={`${conteneur} grid gap-gutter py-space-lg sm:grid-cols-2 lg:grid-cols-4`}>
          {pointsDeConfiance.map((p) => (
            <li key={p.titre} className="flex items-start gap-space-md">
              <p.icon size={20} className="mt-1 shrink-0 text-primary-container" aria-hidden="true" />
              <div>
                <p className="font-label-md text-label-md font-semibold text-navy">{p.titre}</p>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">{p.texte}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* 3. COMPTES / OFFRES — produits vitrine */}
      <section aria-label="Comptes et offres" className={conteneur}>
        <div className="py-space-xl">
          <h2 className="text-headline-section-mobile font-light text-navy lg:text-headline-section">
            Des comptes simples, aux tarifs clairs
          </h2>
          <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
            Chaque offre précise ce qu&apos;elle inclut. Les offres sont présentées à titre de
            vitrine en attendant la mise en service.
          </p>
          <div className="mt-space-xl grid gap-gutter lg:grid-cols-3">
            {offres.map((o) => (
              <article
                key={o.nom}
                className="flex flex-col rounded-lg border border-divider bg-surface-container-lowest p-space-lg"
              >
                <div className="flex items-start justify-between gap-space-md">
                  <h3 className="font-headline-sm text-headline-sm font-semibold text-navy">{o.nom}</h3>
                  <span className="shrink-0 rounded-lg bg-[#8DB8E8]/15 px-2 py-1 font-label-sm text-label-sm text-navy">
                    Vitrine
                  </span>
                </div>
                <p className="mt-space-md font-headline-md text-headline-md font-semibold text-navy">{o.prix}</p>
                <p className="font-body-sm text-body-sm text-neutral-core">{o.prixDetail}</p>
                <ul className="mt-space-lg flex-1 space-y-space-sm">
                  {o.fonctionnalites.map((f) => (
                    <li key={f} className="flex items-start gap-space-sm font-body-md text-body-md text-neutral-core">
                      <Check size={16} className="mt-1 shrink-0 text-access-teal" aria-hidden="true" />
                      {f}
                    </li>
                  ))}
                </ul>
                <Link
                  href="/inscription"
                  className="mt-space-lg inline-flex h-10 w-full items-center justify-center rounded-lg border border-primary-container bg-transparent px-space-lg font-label-md text-label-md text-primary-container hover:bg-surface-tint focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-container"
                >
                  {o.cta}
                </Link>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* 4. POURQUOI ORA BANK */}
      <section aria-label="Pourquoi Ora Bank" className="border-y border-divider bg-surface-tint">
        <div className={conteneur}>
          <div className="py-space-xl">
            <h2 className="text-headline-section-mobile font-light text-navy lg:text-headline-section">
              Pourquoi Ora Bank
            </h2>
            <div className="mt-space-xl grid gap-gutter sm:grid-cols-2 lg:grid-cols-4">
              {argumentsPoursuivre.map((a) => (
                <div key={a.titre}>
                  <a.icon size={24} className="text-primary-container" aria-hidden="true" />
                  <h3 className="mt-space-md font-headline-sm text-headline-sm font-semibold text-navy">{a.titre}</h3>
                  <p className="mt-space-sm font-body-md text-body-md text-neutral-core">{a.texte}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 5. SÉCURITÉ — bande navy */}
      <section aria-label="Sécurité" className="bg-navy text-white">
        <div className={conteneur}>
          <div className="py-space-xl">
            <h2 className="text-headline-section-mobile font-light text-white lg:text-headline-section">
              La sécurité au centre du service
            </h2>
            <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-[#E5E8EE]">
              Chaque étape sensible est contrôlée et documentée, de l&apos;ouverture du compte jusqu&apos;aux virements.
            </p>
            <div className="mt-space-xl grid gap-gutter sm:grid-cols-2 lg:grid-cols-4">
              {mesuresSecurite.map((m) => (
                <div key={m.titre} className="rounded-lg border border-white/20 p-space-lg">
                  <m.icon size={24} className="text-[#8DB8E8]" aria-hidden="true" />
                  <h3 className="mt-space-md font-headline-sm text-headline-sm font-semibold">{m.titre}</h3>
                  <p className="mt-space-sm font-body-sm text-body-sm text-white/80">{m.texte}</p>
                </div>
              ))}
            </div>
            <Link
              href="/securite"
              className="mt-space-xl inline-flex h-10 items-center rounded-lg border border-white/40 px-space-lg font-label-md text-label-md text-white hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Consulter le centre de sécurité
            </Link>
          </div>
        </div>
      </section>

      {/* 6. CHIFFRES — sobres, aucune donnée inventée */}
      <section aria-label="Chiffres clés">
        <ul className={`${conteneur} grid grid-cols-2 gap-gutter py-space-xl lg:grid-cols-4`}>
          {chiffres.map((c) => (
            <li key={c.libelle} className="border-l-2 border-primary-container pl-space-md">
              <p className="tabular font-headline-md text-headline-md font-semibold text-navy">{c.valeur}</p>
              <p className="font-body-sm text-body-sm text-neutral-core">{c.libelle}</p>
            </li>
          ))}
        </ul>
      </section>

      {/* 7. TÉMOIGNAGES — génériques, sans nom d'entreprise */}
      <section aria-label="Témoignages" className="border-y border-divider bg-surface-tint">
        <div className={conteneur}>
          <div className="py-space-xl">
            <h2 className="text-headline-section-mobile font-light text-navy lg:text-headline-section">
              Ce que disent nos clients
            </h2>
            <p className="mt-space-sm font-body-sm text-body-sm text-neutral-core">
              Retours d&apos;expérience illustratifs.
            </p>
            <div className="mt-space-xl grid gap-gutter lg:grid-cols-3">
              {temoignages.map((t) => (
                <figure
                  key={t.initiales}
                  className="rounded-lg border border-divider bg-surface-container-lowest p-space-lg"
                >
                  <blockquote className="font-body-md text-body-md text-neutral-core">
                    « {t.citation} »
                  </blockquote>
                  <figcaption className="mt-space-lg flex items-center gap-space-md">
                    <span className="flex h-9 w-9 items-center justify-center rounded-full bg-navy font-label-md text-label-md tabular text-white">
                      {t.initiales}
                    </span>
                    <span className="font-label-md text-label-md text-navy">
                      {t.initiales} {t.prenom} — {t.ville}
                    </span>
                  </figcaption>
                </figure>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 8. FAQ — accordéons natives <details>, sans JavaScript */}
      <section aria-label="Questions fréquentes" className={conteneur}>
        <div className="py-space-xl">
          <h2 className="text-headline-section-mobile font-light text-navy lg:text-headline-section">
            Questions fréquentes
          </h2>
          <div className="mt-space-xl max-w-3xl divide-y divide-divider rounded-lg border border-divider bg-surface-container-lowest">
            {faq.map((q) => (
              <details key={q.question} className="group">
                <summary className="cursor-pointer list-none px-space-lg py-space-md font-label-md text-label-md font-semibold text-navy hover:bg-surface-tint focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-primary-container">
                  {q.question}
                </summary>
                <p className="px-space-lg pb-space-md font-body-md text-body-md text-neutral-core">
                  {q.reponse}
                </p>
              </details>
            ))}
          </div>
          <Link
            href="/faq"
            className="mt-space-lg inline-block font-label-md text-label-md text-primary-container hover:underline"
          >
            Voir toutes les questions →
          </Link>
        </div>
      </section>

      {/* 9. DERNIER APPEL — bande navy finale */}
      <section aria-label="Ouvrir un compte" className="bg-navy text-white">
        <div className={`${conteneur} flex flex-col items-center gap-space-lg py-space-2xl text-center`}>
          <h2 className="max-w-2xl text-balance text-headline-section-mobile font-light text-white lg:text-headline-section">
            Prêt à rejoindre Ora Bank ? L&apos;ouverture de votre compte commence ici.
          </h2>
          <div className="flex flex-col justify-center gap-space-md sm:flex-row">
            <Link
              href="/inscription"
              className="inline-flex h-10 w-full items-center justify-center rounded-lg bg-primary-container px-space-lg font-label-md text-label-md text-white hover:bg-[#2C5A92] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white sm:w-auto"
            >
              Ouvrir un compte
            </Link>
            <Link
              href="/connexion"
              className="inline-flex h-10 w-full items-center justify-center rounded-lg bg-access-teal px-space-lg font-label-md text-label-md text-white hover:bg-[#307272] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white sm:w-auto"
            >
              Se connecter
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
