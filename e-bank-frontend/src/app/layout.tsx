import type { Metadata } from "next";
import { Source_Sans_3 } from "next/font/google";
import "./globals.css";
import { Fournisseurs } from "./providers";

const sourceSans = Source_Sans_3({
  subsets: ["latin"],
  weight: ["300", "400", "600", "700"],
  variable: "--font-source-sans",
});

export const metadata: Metadata = {
  title: "Ora Bank — Banque en ligne",
  description:
    "Gérez vos comptes, virements SEPA, bénéficiaires, relevés et réclamations en toute sécurité.",
};

export default function MiseEnPageRacine({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="fr" className={sourceSans.variable}>
      <body className="font-sans">
        <Fournisseurs>{children}</Fournisseurs>
      </body>
    </html>
  );
}

export { MiseEnPageRacine as RootLayout };
