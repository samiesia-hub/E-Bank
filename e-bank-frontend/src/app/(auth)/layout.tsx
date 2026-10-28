// Auth layout — split screen from designs/... (4) & (6):
// navy brand panel on the left, form column on the right.
import { Shield } from "lucide-react";

export default function MiseEnPageAuth({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen w-full bg-surface-container-low lg:grid lg:grid-cols-12">
      {/* Brand panel — hidden below lg, the form takes the full width there. */}
      <aside className="hidden bg-navy p-space-xl text-white lg:col-span-5 lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-space-sm">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-container">
            <Shield className="h-6 w-6 text-white" />
          </div>
          <div className="leading-tight">
            <span className="block font-headline-md text-headline-md font-bold text-white">
              Bankati
            </span>
            <span className="block font-label-sm text-label-sm text-surface-dim">
              Banque Digitale
            </span>
          </div>
        </div>

        <div className="my-auto max-w-sm py-space-xl">
          <h1 className="mb-space-xs font-headline-section text-headline-section font-light text-white">
            Banque privée &amp; de détail
          </h1>
          <p className="font-body-md text-body-md text-surface-dim">
            Votre banque, simple et digitale. Une gestion financière rigoureuse
            au service de vos ambitions.
          </p>

          {/* Illustration: a phone with a shield, drawn with plain divs. */}
          <div className="relative mt-space-lg flex h-56 w-full items-center justify-center rounded-lg bg-white/5">
            <div className="absolute h-44 w-44 rounded-full bg-primary-container/20" />
            <div className="absolute h-32 w-32 rounded-full bg-secondary-fixed/10" />
            <div className="relative z-10 flex h-40 w-24 flex-col items-center justify-between rounded-xl bg-white/10 p-space-xs">
              <div className="mt-1 h-1 w-6 rounded bg-white/40" />
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-container">
                <Shield className="h-6 w-6 text-white" />
              </div>
              <div className="mb-1 h-3 w-3 rounded-full bg-white/30" />
            </div>
          </div>
        </div>

        <div className="pt-space-md">
          <p className="font-label-sm text-label-sm text-surface-dim/70">
            © 2026 Bankati S.A. Établissement de crédit agréé et régulé.
          </p>
        </div>
      </aside>

      <main className="flex min-h-screen w-full flex-col items-center justify-center p-space-md md:p-space-xl lg:col-span-7">
        <div className="w-full max-w-md">{children}</div>
      </main>
    </div>
  );
}

export { MiseEnPageAuth as AuthLayout };
