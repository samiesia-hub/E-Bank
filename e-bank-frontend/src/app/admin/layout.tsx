import { Montserrat } from "next/font/google";
import { BarreHaute } from "@/components/admin/BarreHaute";
import { BarreLaterale } from "@/components/admin/BarreLaterale";

// Police du back-office (le portail client garde la sienne)
const montserrat = Montserrat({
    subsets: ["latin"],
    weight: ["400", "500", "600", "700"],
});

// Portail admin — réservé au rôle ADMIN (voir src/proxy.ts)
export default function MiseEnPageAdmin({
                                            children,
                                        }: {
    children: React.ReactNode;
}) {
    return (
        <div className={`${montserrat.className} flex min-h-screen flex-col bg-admin-page`}>
            <BarreHaute />
            <div className="flex flex-1">
                <BarreLaterale />
                <div className="min-w-0 flex-1">{children}</div>
            </div>
        </div>
    );
}

export { MiseEnPageAdmin as AdminLayout };