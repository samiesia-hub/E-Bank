// Wizard virement en 4 étapes: bénéficiaire → montant → résumé → OTP → reçu
// State local avec Zustand (features/transfers/store), include: verifier 2FA + consulter bénéficiaires
// Sécurité: clé idempotence + bouton désactivé pendant envoi
export default function PageNouveauVirement() {
  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">Virement SEPA</p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">Effectuer un virement</h1>
      <ol className="mt-space-lg grid gap-space-md md:grid-cols-4">
        {["1. Bénéficiaire", "2. Montant & motif", "3. Vérification OTP", "4. Reçu"].map((s) => (
          <li key={s} className="rounded-lg border border-divider bg-surface-container-lowest p-space-md font-body-sm text-body-sm text-neutral-core">{s}</li>
        ))}
      </ol>
      <p className="mt-space-md font-body-sm text-body-sm text-neutral-core">TODO: brancher features/transfers (React Hook Form + Zod + IBAN + plafonds).</p>
    </main>
  );
}
