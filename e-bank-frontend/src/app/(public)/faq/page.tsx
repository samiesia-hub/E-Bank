import Link from "next/link";
import { ChevronDown, LifeBuoy } from "lucide-react";

// Aide / FAQ publique — réponses reprises du fonctionnement réel de l'app
// (ouverture de compte, OTP, KYC, virements, opposition, support).
// Aucune condition commerciale n'est annoncée ici : les tarifs ne sont pas dans l'app.

const conteneur = "mx-auto w-full max-w-[1200px] px-margin";

type Rubrique = {
  titre: string;
  questions: { question: string; reponse: string }[];
};

const rubriques: Rubrique[] = [
  {
    titre: "Ouvrir un compte",
    questions: [
      {
        question: "Comment devenir client ?",
        reponse:
          "Depuis la page d'inscription, renseignez vos informations personnelles, confirmez le code reçu par e-mail, puis transmettez une pièce d'identité. Votre compte est activé après la vérification d'identité (KYC) par notre équipe.",
      },
      {
        question: "Quels documents faut-il fournir ?",
        reponse:
          "Une pièce d'identité en cours de validité est nécessaire pour la vérification KYC. Si un document est refusé, le motif est indiqué dans votre espace : vous pouvez le remplacer et le renvoyer.",
      },
      {
        question: "Combien de temps prend la vérification ?",
        reponse:
          "Le dossier passe en revue par l'équipe support. Une fois validé, vous recevez une confirmation et votre compte devient opérationnel pour les virements.",
      },
    ],
  },
  {
    titre: "Connexion et sécurité",
    questions: [
      {
        question: "J'ai oublié mon mot de passe, que faire ?",
        reponse:
          "Sur la page de connexion, choisissez « Mot de passe oublié ». Vous recevez un code à usage unique par e-mail, puis vous définissez un nouveau mot de passe (12 caractères minimum, une majuscule, une minuscule et un chiffre).",
      },
      {
        question: "Pourquoi un code à chaque opération sensible ?",
        reponse:
          "La connexion et les virements sont protégés par une authentification forte : un code à usage unique (OTP) confirme que c'est bien vous. Le code expire rapidement et ne sert qu'une fois.",
      },
      {
        question: "Que se passe-t-il si je me déconnecte ?",
        reponse:
          "La session est fermée côté serveur : le cookie de connexion est invalidé et ne peut plus être réutilisé, même depuis un autre navigateur.",
      },
    ],
  },
  {
    titre: "Virements et bénéficiaires",
    questions: [
      {
        question: "Comment ajouter un bénéficiaire ?",
        reponse:
          "Dans votre espace client, rubrique Bénéficiaires, ajoutez un nom et un IBAN ou RIB. Le format du compte est contrôlé à l'enregistrement, et un même compte ne peut pas être enregistré deux fois.",
      },
      {
        question: "Comment envoyer un virement ?",
        reponse:
          "Suivez l'assistant de virement : choix du compte débité, du bénéficiaire, du montant et du motif, puis confirmation par code OTP. Un reçu détaillé avec une référence unique est généré pour chaque virement exécuté.",
      },
      {
        question: "Un virement peut-il échouer en cours de route ?",
        reponse:
          "Le débit et le crédit sont traités ensemble : si une étape échoue, rien n'est débité. En cas de refus (solde insuffisant, compte bloqué), le motif est affiché et le virement n'est pas enregistré.",
      },
      {
        question: "Où retrouver l'historique ?",
        reponse:
          "La page Virements liste vos opérations avec filtres et pagination. Vous pouvez ouvrir le détail de chaque virement et son reçu.",
      },
    ],
  },
  {
    titre: "Comptes, cartes et support",
    questions: [
      {
        question: "Quels types de comptes sont proposés ?",
        reponse:
          "L'application gère les comptes courants, les comptes épargne et les comptes professionnels, chacun avec son IBAN et son solde.",
      },
      {
        question: "Comment faire opposition sur ma carte ?",
        reponse:
          "L'opposition est demandée depuis votre espace client ou depuis la page de connexion en cas d'urgence. La demande est traitée immédiatement, 24 h sur 24.",
      },
      {
        question: "Comment contacter le support ?",
        reponse:
          "Créez un ticket depuis votre espace client : vous suivez son état (ouvert, en cours, résolu) et les réponses de l'équipe. Pour un litige sur une opération, utilisez plutôt une réclamation, qui suit son propre circuit.",
      },
    ],
  },
];

export const metadata = {
  title: "Aide et questions fréquentes — Ora Bank",
  description:
    "Réponses sur l'ouverture de compte, la connexion, les virements et le support Ora Bank.",
};

export default function PageFaq() {
  return (
    <main>
      <section aria-label="Aide" className="border-b border-divider bg-surface-tint">
        <div className={`${conteneur} py-space-2xl`}>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
            Aide
          </p>
          <h1 className="mt-space-md max-w-2xl text-balance font-display-hero text-display-hero-mobile text-navy lg:text-display-hero">
            Questions fréquentes
          </h1>
          <p className="mt-space-md max-w-xl font-body-lg text-body-lg text-neutral-core">
            Ouverture de compte, connexion, virements, carte et support : les réponses aux
            questions les plus courantes.
          </p>
        </div>
      </section>

      <div className={`${conteneur} grid gap-space-xl py-space-2xl lg:grid-cols-[240px_1fr]`}>
        <nav aria-label="Rubriques" className="lg:sticky lg:top-space-lg lg:self-start">
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-neutral-core">
            Rubriques
          </p>
          <ul className="mt-space-md space-y-space-sm">
            {rubriques.map((rubrique, index) => (
              <li key={rubrique.titre}>
                <a
                  href={`#rubrique-${index}`}
                  className="font-body-sm text-body-sm text-navy hover:text-primary-container"
                >
                  {rubrique.titre}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="space-y-space-2xl">
          {rubriques.map((rubrique, index) => (
            <section
              key={rubrique.titre}
              id={`rubrique-${index}`}
              aria-label={rubrique.titre}
              className="scroll-mt-space-xl"
            >
              <h2 className="font-headline-section-mobile text-headline-section-mobile font-light text-navy lg:text-headline-section">
                {rubrique.titre}
              </h2>
              <div className="mt-space-lg divide-y divide-divider border-y border-divider">
                {rubrique.questions.map((item) => (
                  <details key={item.question} className="group py-space-md">
                    <summary className="flex cursor-pointer list-none items-center justify-between gap-space-md font-label-md text-label-md font-semibold text-navy marker:hidden">
                      {item.question}
                      <ChevronDown
                        size={18}
                        className="shrink-0 text-neutral-core transition-transform group-open:rotate-180"
                        aria-hidden="true"
                      />
                    </summary>
                    <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
                      {item.reponse}
                    </p>
                  </details>
                ))}
              </div>
            </section>
          ))}

          <section
            aria-label="Contacter le support"
            className="flex flex-col gap-space-md rounded border border-divider bg-surface-container-low p-space-lg sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start gap-space-md">
              <LifeBuoy size={22} className="mt-1 shrink-0 text-primary-container" aria-hidden="true" />
              <div>
                <p className="font-label-md text-label-md font-semibold text-navy">
                  Votre question n&apos;est pas ici ?
                </p>
                <p className="mt-space-xs font-body-sm text-body-sm text-neutral-core">
                  Créez un ticket depuis votre espace client : l&apos;équipe support suit
                  chaque demande jusqu&apos;à sa résolution.
                </p>
              </div>
            </div>
            <Link
              href="/connexion"
              className="inline-flex h-10 shrink-0 items-center justify-center rounded-lg bg-primary-container px-space-lg font-label-md text-label-md text-white hover:bg-[#2C5A92]"
            >
              Accéder à mon espace
            </Link>
          </section>
        </div>
      </div>
    </main>
  );
}
