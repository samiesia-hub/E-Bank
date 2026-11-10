import Link from "next/link";
import {
  ArrowRight,
  CreditCard,
  FileText,
  Landmark,
  MessagesSquare,
  PieChart,
  Send,
  ShieldCheck,
  Smartphone,
  UserPlus,
  Zap,
} from "lucide-react";

// Page Services — décrit uniquement ce que l'application fait réellement
// (types de comptes, bénéficiaires, virements OTP, historique, relevés, support).
// Aucun tarif ni taux n'est affiché : ils ne sont pas gérés par l'app.

const conteneur = "mx-auto w-full max-w-[1200px] px-margin";

const comptes = [
  {
    icone: Landmark,
    titre: "Compte courant",
    texte:
      "Le compte du quotidien : salaire, prélèvements et virements, avec un IBAN unique généré à l'ouverture.",
  },
  {
    icone: PieChart,
    titre: "Compte épargne",
    texte:
      "Un compte séparé pour mettre de côté, suivi comme le compte courant avec son propre solde.",
  },
  {
    icone: FileText,
    titre: "Compte professionnel",
    texte:
      "Pour les activités indépendantes et les petites structures, avec les mêmes services de virement.",
  },
];

const operations = [
  {
    icone: Send,
    titre: "Virements SEPA",
    texte:
      "Assistant en quatre étapes, bénéficiaires enregistrés, confirmation par code OTP et reçu avec référence unique.",
    points: [
      "Ajout de bénéficiaire par IBAN ou RIB, contrôlé à l'enregistrement",
      "Débit et crédit traités ensemble : un échec n'entraîne aucun débit",
      "Reçu détaillé consultable pour chaque virement exécuté",
    ],
  },
  {
    icone: CreditCard,
    titre: "Carte et opposition",
    texte:
      "Opposition immédiate depuis votre espace, sans appel ni courrier, 24 h sur 24.",
    points: [
      "Blocage de la carte depuis l'espace client",
      "Accès de secours depuis la page de connexion",
      "Chaque demande est tracée dans le journal d'audit",
    ],
  },
  {
    icone: MessagesSquare,
    titre: "Support et réclamations",
    texte:
      "Un ticket pour une question, une réclamation pour un litige : les deux sont suivis jusqu'à la résolution.",
    points: [
      "Tickets avec état et prise en charge par un agent",
      "Réclamations avec statut et historique des échanges",
      "Réponses conservées dans votre espace",
    ],
  },
];

const parcours = [
  {
    icone: UserPlus,
    numero: "01",
    titre: "Vous ouvrez le compte",
    texte:
      "Inscription en ligne, code de confirmation par e-mail, puis vérification d'identité (KYC).",
  },
  {
    icone: Smartphone,
    numero: "02",
    titre: "Vous accédez à votre espace",
    texte:
      "Connexion protégée par une authentification forte : un code à usage unique à chaque accès sensible.",
  },
  {
    icone: Zap,
    numero: "03",
    titre: "Vous opérez",
    texte:
      "Virements, bénéficiaires, historique filtrable et relevés : tout se fait depuis le navigateur ou le mobile.",
  },
];

export const metadata = {
  title: "Services — Ora Bank",
  description:
    "Comptes, virements SEPA, carte, historique et support : les services bancaires Ora Bank en ligne.",
};

export default function PageServices() {
  return (
    <main>
      <section aria-label="Nos services" className="border-b border-divider bg-surface-tint">
        <div className={`${conteneur} py-space-2xl`}>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
            Services
          </p>
          <h1 className="mt-space-md max-w-2xl text-balance font-display-hero text-display-hero-mobile text-navy lg:text-display-hero">
            Vos comptes, vos virements et votre suivi, au même endroit.
          </h1>
          <p className="mt-space-md max-w-xl font-body-lg text-body-lg text-neutral-core">
            Une banque de détail complète, en ligne : ouverture de compte, virements SEPA,
            historique, relevés et support humain.
          </p>
          <div className="mt-space-xl flex flex-col gap-space-md sm:flex-row">
            <Link
              href="/inscription"
              className="inline-flex h-10 w-full items-center justify-center rounded-lg bg-primary-container px-space-lg font-label-md text-label-md text-white hover:bg-[#2C5A92] sm:w-auto"
            >
              Ouvrir un compte
            </Link>
            <Link
              href="/connexion"
              className="inline-flex h-10 w-full items-center justify-center rounded-lg border border-navy px-space-lg font-label-md text-label-md text-navy hover:bg-navy hover:text-white sm:w-auto"
            >
              Accès client
            </Link>
          </div>
        </div>
      </section>

      <section aria-label="Types de comptes" className="border-b border-divider">
        <div className={`${conteneur} py-space-2xl`}>
          <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy lg:text-headline-section">
            Trois types de comptes
          </h2>
          <ul className="mt-space-xl grid gap-gutter md:grid-cols-3">
            {comptes.map((compte) => (
              <li key={compte.titre} className="border-t border-navy pt-space-md">
                <compte.icone size={24} className="text-primary-container" aria-hidden="true" />
                <h3 className="mt-space-sm font-label-md text-label-md font-semibold text-navy">
                  {compte.titre}
                </h3>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                  {compte.texte}
                </p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section aria-label="Opérations" className="border-b border-divider">
        <div className={`${conteneur} grid gap-space-xl py-space-2xl lg:grid-cols-3`}>
          {operations.map((operation) => (
            <article key={operation.titre}>
              <operation.icone
                size={26}
                className="text-primary-container"
                aria-hidden="true"
              />
              <h2 className="mt-space-sm font-headline-section-mobile text-headline-section-mobile font-light text-navy">
                {operation.titre}
              </h2>
              <p className="mt-space-sm font-body-md text-body-md text-neutral-core">
                {operation.texte}
              </p>
              <ul className="mt-space-md space-y-space-sm">
                {operation.points.map((point) => (
                  <li
                    key={point}
                    className="flex items-start gap-space-sm font-body-sm text-body-sm text-navy"
                  >
                    <ShieldCheck
                      size={16}
                      className="mt-0.5 shrink-0 text-access-teal"
                      aria-hidden="true"
                    />
                    {point}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>

      <section aria-label="Parcours client" className="border-b border-divider bg-surface-container-low">
        <div className={`${conteneur} py-space-2xl`}>
          <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy lg:text-headline-section">
            De l&apos;ouverture au premier virement
          </h2>
          <ol className="mt-space-xl grid gap-gutter md:grid-cols-3">
            {parcours.map((etape) => (
              <li key={etape.numero} className="border-t border-divider pt-space-md">
                <span className="font-label-sm text-label-sm tracking-wider text-neutral-core">
                  {etape.numero}
                </span>
                <etape.icone
                  size={22}
                  className="mt-space-sm text-access-teal"
                  aria-hidden="true"
                />
                <h3 className="mt-space-sm font-label-md text-label-md font-semibold text-navy">
                  {etape.titre}
                </h3>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                  {etape.texte}
                </p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section aria-label="Passer à l'action">
        <div className={`${conteneur} flex flex-col gap-space-md py-space-2xl sm:flex-row sm:items-center sm:justify-between`}>
          <div>
            <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy">
              Prêt à ouvrir votre compte ?
            </h2>
            <p className="mt-space-xs font-body-md text-body-md text-neutral-core">
              L&apos;ouverture se fait entièrement en ligne, en quelques minutes.
            </p>
          </div>
          <Link
            href="/inscription"
            className="inline-flex h-10 shrink-0 items-center justify-center gap-space-xs rounded-lg bg-primary-container px-space-lg font-label-md text-label-md text-white hover:bg-[#2C5A92]"
          >
            Devenir client
            <ArrowRight size={16} aria-hidden="true" />
          </Link>
        </div>
      </section>
    </main>
  );
}
