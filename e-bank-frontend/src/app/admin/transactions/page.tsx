"use client";

// Transactions (admin) — supervision paginée /api/admin/transactions + export CSV
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/interface/Badge";
import { Bouton } from "@/components/interface/Bouton";
import { ChampSaisie } from "@/components/interface/ChampSaisie";
import { TableauDonnees, type DataTableColumn } from "@/components/interface/TableauDonnees";
import { messageErreurApi } from "@/lib/client-api";
import { formaterDateHeure, formaterMontant } from "@/lib/formatage";
import {
  exporterTransactionsCsv,
  rechercherTransactionsAdmin,
  type FiltresTransactions,
  type SensTransaction,
  type TransactionAdmin,
} from "@/lib/api/admin-transactions";
import type { StatutTransaction } from "@/types/banque";

const TAILLE_PAGE = 20;

type BrouillonFiltres = {
  du: string;
  au: string;
  sens: "" | SensTransaction;
  clientId: string;
  compteId: string;
};

const BROUILLON_VIDE: BrouillonFiltres = { du: "", au: "", sens: "", clientId: "", compteId: "" };

const TONS_STATUT: Record<StatutTransaction, "success" | "pending" | "error"> = {
  EXECUTEE: "success",
  EN_ATTENTE: "pending",
  REJETEE: "error",
};

export default function PageTransactionsAdmin() {
  const [brouillon, setBrouillon] = useState<BrouillonFiltres>(BROUILLON_VIDE);
  const [filtres, setFiltres] = useState<FiltresTransactions>({});
  const [page, setPage] = useState(0);
  const [erreurFiltres, setErreurFiltres] = useState<string | null>(null);
  const [exportEnCours, setExportEnCours] = useState(false);
  const [erreurExport, setErreurExport] = useState<string | null>(null);
  const [succesExport, setSuccesExport] = useState<string | null>(null);

  const transactions = useQuery({
    queryKey: ["admin", "transactions", filtres, page],
    queryFn: () => rechercherTransactionsAdmin({ ...filtres, page, size: TAILLE_PAGE }),
  });

  function appliquerFiltres(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setSuccesExport(null);

    const clientId = entier(brouillon.clientId);
    if (clientId === "invalide") {
      setErreurFiltres("L'identifiant client doit être un entier positif.");
      return;
    }
    const compteId = entier(brouillon.compteId);
    if (compteId === "invalide") {
      setErreurFiltres("L'identifiant de compte doit être un entier positif.");
      return;
    }
    if (brouillon.du && brouillon.au && brouillon.du > brouillon.au) {
      setErreurFiltres("La date de début doit précéder la date de fin.");
      return;
    }

    setErreurFiltres(null);
    setFiltres({
      du: brouillon.du || undefined,
      au: brouillon.au || undefined,
      sens: brouillon.sens || undefined,
      clientId: clientId ?? undefined,
      compteId: compteId ?? undefined,
    });
    setPage(0);
  }

  function reinitialiser() {
    setBrouillon(BROUILLON_VIDE);
    setFiltres({});
    setPage(0);
    setErreurFiltres(null);
    setSuccesExport(null);
  }

  async function exporter() {
    setExportEnCours(true);
    setErreurExport(null);
    setSuccesExport(null);
    try {
      const nomFichier = await exporterTransactionsCsv(filtres);
      setSuccesExport(`Fichier ${nomFichier} téléchargé.`);
    } catch (erreur) {
      setErreurExport(messageErreurApi(erreur));
    } finally {
      setExportEnCours(false);
    }
  }

  const donnees = transactions.data;
  const colonnes: DataTableColumn<TransactionAdmin>[] = [
    {
      key: "date",
      entete: "Date",
      rendu: (transaction) =>
        transaction.dateTransaction ? formaterDateHeure(transaction.dateTransaction) : "—",
    },
    {
      key: "emetteur",
      entete: "Émetteur",
      rendu: (transaction) => transaction.emetteur ?? "—",
    },
    {
      key: "destinataire",
      entete: "Destinataire",
      rendu: (transaction) => transaction.destinataire ?? "—",
    },
    {
      key: "compte",
      entete: "Compte",
      rendu: (transaction) => (
        <span className="tabular-nums">{transaction.numeroCompte ?? "—"}</span>
      ),
    },
    {
      key: "montant",
      entete: "Montant",
      numerique: true,
      rendu: (transaction) => (
        <span className={transaction.sens === "CREDIT" ? "text-access-teal" : "text-on-surface"}>
          {transaction.sens === "CREDIT" ? "+ " : transaction.sens === "DEBIT" ? "− " : ""}
          {formaterMontant({
            montant: String(transaction.montant),
            devise: transaction.devise ?? "MAD",
          })}
        </span>
      ),
    },
    {
      key: "statut",
      entete: "Statut",
      rendu: (transaction) =>
        transaction.statutTransaction ? (
          <Badge tone={TONS_STATUT[transaction.statutTransaction] ?? "neutral"}>
            {transaction.statutTransaction}
          </Badge>
        ) : (
          "—"
        ),
    },
    {
      key: "libelle",
      entete: "Libellé",
      rendu: (transaction) => transaction.libelle ?? "—",
    },
  ];

  return (
    <main className="flex flex-col gap-space-xl px-margin py-space-xl">
      <div className="flex flex-wrap items-end justify-between gap-space-md">
        <div>
          <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
            Bankati — Transactions
          </p>
          <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">Transactions</h1>
          <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
            Supervision de tous les mouvements de la banque, filtrables par période, sens et client.
          </p>
        </div>
        <Bouton variant="outline" onClick={exporter} disabled={exportEnCours}>
          {exportEnCours ? "Export en cours…" : "Exporter en CSV"}
        </Bouton>
      </div>

      {erreurExport && (
        <p role="alert" className="font-body-sm text-body-sm text-error">
          {erreurExport}
        </p>
      )}
      {succesExport && (
        <p role="status" className="font-body-sm text-body-sm text-access-teal">
          {succesExport}
        </p>
      )}

      <section
        aria-label="Filtres des transactions"
        className="rounded-lg border border-divider bg-surface-container-lowest p-space-lg"
      >
        <form onSubmit={appliquerFiltres} className="grid gap-space-md md:grid-cols-2 xl:grid-cols-5">
          <ChampSaisie
            label="Du"
            name="du"
            type="date"
            value={brouillon.du}
            onChange={(e) => setBrouillon({ ...brouillon, du: e.target.value })}
          />
          <ChampSaisie
            label="Au"
            name="au"
            type="date"
            value={brouillon.au}
            onChange={(e) => setBrouillon({ ...brouillon, au: e.target.value })}
          />
          <div className="flex flex-col gap-space-xs">
            <label htmlFor="sens" className="font-label-md text-label-md text-on-surface">
              Sens
            </label>
            <select
              id="sens"
              value={brouillon.sens}
              onChange={(e) =>
                setBrouillon({ ...brouillon, sens: e.target.value as "" | SensTransaction })
              }
              className="h-10 rounded-lg border border-divider bg-surface-container-lowest px-3 font-body-md text-body-md text-navy focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            >
              <option value="">Tous</option>
              <option value="DEBIT">Débit</option>
              <option value="CREDIT">Crédit</option>
            </select>
          </div>
          <ChampSaisie
            label="Client (id)"
            name="clientId"
            type="number"
            min={1}
            step={1}
            value={brouillon.clientId}
            onChange={(e) => setBrouillon({ ...brouillon, clientId: e.target.value })}
          />
          <ChampSaisie
            label="Compte (id)"
            name="compteId"
            type="number"
            min={1}
            step={1}
            value={brouillon.compteId}
            onChange={(e) => setBrouillon({ ...brouillon, compteId: e.target.value })}
          />

          <div className="flex items-center gap-space-sm md:col-span-2 xl:col-span-5">
            <Bouton type="submit">Filtrer</Bouton>
            <Bouton type="button" variant="ghost" onClick={reinitialiser}>
              Réinitialiser
            </Bouton>
          </div>
        </form>

        {erreurFiltres && (
          <p role="alert" className="mt-space-md font-body-sm text-body-sm text-error">
            {erreurFiltres}
          </p>
        )}
      </section>

      <section aria-label="Liste des transactions" className="flex flex-col gap-space-md">
        {transactions.isLoading && (
          <p className="font-body-md text-body-md text-neutral-core">Chargement des transactions…</p>
        )}

        {transactions.isError && (
          <p role="alert" className="font-body-md text-body-md text-error">
            {messageErreurApi(transactions.error)}
          </p>
        )}

        {donnees && (
          <>
            <TableauDonnees
              colonnes={colonnes}
              lignes={donnees.contenu}
              legende="Transactions de la banque"
              messageVide="Aucune transaction ne correspond à ces filtres."
            />

            <div className="flex flex-wrap items-center justify-between gap-space-md">
              <p className="font-body-sm text-body-sm text-neutral-core">
                {donnees.totalElements} transaction{donnees.totalElements > 1 ? "s" : ""} — page{" "}
                {donnees.totalPages === 0 ? 0 : donnees.page + 1} sur {donnees.totalPages}
              </p>
              <div className="flex items-center gap-space-sm">
                <Bouton
                  variant="outline"
                  disabled={page === 0 || transactions.isFetching}
                  onClick={() => setPage((precedente) => Math.max(0, precedente - 1))}
                >
                  Précédent
                </Bouton>
                <Bouton
                  variant="outline"
                  disabled={donnees.dernierePage || transactions.isFetching}
                  onClick={() => setPage((precedente) => precedente + 1)}
                >
                  Suivant
                </Bouton>
              </div>
            </div>
          </>
        )}
      </section>
    </main>
  );
}

/** "" -> undefined, entier positif -> number, sinon "invalide". */
function entier(valeur: string): number | undefined | "invalide" {
  if (valeur.trim() === "") return undefined;
  const nombre = Number(valeur);
  return Number.isInteger(nombre) && nombre > 0 ? nombre : "invalide";
}
