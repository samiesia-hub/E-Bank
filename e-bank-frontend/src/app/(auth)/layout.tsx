// Auth layout — centered card, Teal CTA (DESIGN.md: Access = login only)
export default function MiseEnPageAuth({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-surface-tint">
      <header className="bg-navy py-space-md text-center text-white">
        <span className="font-headline-sm text-headline-sm font-semibold">Ora Bank — Espace sécurisé</span>
      </header>
      <main className="flex flex-1 items-center justify-center px-margin py-space-xl">
        <div className="w-full max-w-md rounded-lg border border-divider bg-surface-container-lowest p-space-lg">
          {children}
        </div>
      </main>
    </div>
  );
}

export { MiseEnPageAuth as AuthLayout };
