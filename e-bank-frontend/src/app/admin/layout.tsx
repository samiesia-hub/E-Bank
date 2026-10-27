import { BarreHaute } from "@/components/admin/BarreHaute";
import { BarreLaterale } from "@/components/admin/BarreLaterale";

// Portail admin — réservé au rôle ADMIN (voir src/proxy.ts)
export default function MiseEnPageAdmin({
                                          children,
                                        }: {
  children: React.ReactNode;
}) {
  return (
      <div className="flex min-h-screen flex-col bg-admin-page">
        <BarreHaute />
        <div className="flex flex-1">
          <BarreLaterale />
          <div className="min-w-0 flex-1">{children}</div>
        </div>
      </div>
  );
}

export { MiseEnPageAdmin as AdminLayout };