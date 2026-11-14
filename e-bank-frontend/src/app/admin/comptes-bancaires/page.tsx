"use client";

// Comptes bancaires (admin) — liste, ouverture et gel/réactivation via /api/admin/comptes
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge } from "@/components/interface/Badge";
import { Bouton } from "@/components/interface/Bouton";
import { ChampSaisie } from "@/components/interface/ChampSaisie";
import { TableauDonnees, type DataTableColumn } from "@/components/interface/TableauDonnees";
import { formaterDate, formaterIban, formaterMontant } from "@/lib/formatage";
import { changerStatutCompte, creerCompte, listerComptesAdmin, messageErreur } from "@/services/compteService";
import type { CompteAdmin } from "@/services/compteService";
import type { StatutCompte, TypeCompte } from "@/types/banque";

const TYPES_COMPTE: TypeCompte[] = ["COURANT", "EPARGNE", "PROFESSIONNEL"];
const CLE_LISTE = ["admin", "comptes"] as const;

export default function PageComptesBancaires() {
  const queryClient = useQueryClient();
  const [clientId, setClientId] = useState("");
  const [typeCompte, setTypeCompte] = useState<TypeCompte>("COURANT");
  const [soldeInitial, setSoldeInitial] = useState("0");
  const [rib, setRib] = useState("");
  const [erreurFormulaire, setErreurFormulaire] = useState<string | null>(null);
  const [succes, setSucces] = useState<string | null>(null);

  const comptes = useQuery({
    queryKey: CLE_LISTE,
    queryFn: listerComptesAdmin,
  });

  const ouverture = useMutation({
    mutationFn: creerCompte,
    onSuccess: (compte) => {
      setSucces(
        `Compte ${compte.numeroCompte} ouvert (RIB ${compte.rib}, solde ${formaterMontant({
          montant: String(compte.solde),
          devise: compte.devise,
        })}).`
      );
      setErreurFormulaire(null);
      setRib("");
      setClientId("");
      setSoldeInitial("0");
      setTypeCompte("COURANT");
      void queryClient.invalidateQueries({ queryKey: CLE_LISTE });
    },
    onError: (erreur) => {
      setSucces(null);
      setErreurFormulaire(messageErreur(erreur));
    },
  });

  const changementStatut = useMutation({
    mutationFn: ({ id, statut }: { id: number; statut: StatutCompte }) => changerStatutCompte(id, statut),
    onSuccess: (compte) => {
      setSucces(
        compte.statutCompte === "BLOQUE"
          ? `Compte ${compte.numeroCompte} gelé.`
          : `Compte ${compte.numeroCompte} réactivé.`
      );
      setErreurFormulaire(null);
      void queryClient.invalidateQueries({ queryKey: CLE_LISTE });
    },
    onError: (erreur) => {
      setSucces(null);
      setErreurFormulaire(messageErreur(erreur));
    },
  });

  function soumettre(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setSucces(null);

    const client = Number(clientId);
    if (!Number.isInteger(client) || client <= 0) {
      setErreurFormulaire("L'identifiant du client est obligatoire (entier positif).");
      return;
    }

    const solde = Number(soldeInitial === "" ? "0" : soldeInitial);
    if (Number.isNaN(solde) || solde < 0) {
      setErreurFormulaire("Le solde initial doit être un montant positif ou nul.");
      return;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(String(soldeInitial === "" ? "0" : soldeInitial))) {
      setErreurFormulaire("Le solde initial accepte au maximum deux décimales.");
      return;
    }

    ouverture.mutate({
      clientId: client,
      typeCompte,
      soldeInitial: solde,
      rib: rib.trim() === "" ? undefined : rib.trim(),
    });
  }

  const colonnes: DataTableColumn<CompteAdmin>[] = [
    {
      key: "client",
      entete: "Titulaire",
      rendu: (compte) => (
        <div className="flex flex-col">
          <span>{compte.clientNom ?? compte.clientEmail ?? "—"}</span>
          <span className="font-body-sm text-body-sm text-neutral-core">
            {compte.clientEmail ?? `client #${compte.clientId ?? "—"}`}
          </span>
        </div>
      ),
    },
    {
      key: "compte",
      entete: "Compte",
      rendu: (compte) => (
        <div className="flex flex-col">
          <span className="tabular-nums">{compte.numeroCompte}</span>
          <span className="font-body-sm text-body-sm text-neutral-core">{formaterIban(compte.rib)}</span>
        </div>
      ),
    },
    { key: "type", entete: "Type", rendu: (compte) => compte.typeCompte },
    {
      key: "solde",
      entete: "Solde",
      numerique: true,
      rendu: (compte) =>
        formaterMontant({ montant: String(compte.solde), devise: compte.devise }),
    },
    {
      key: "creation",
      entete: "Ouverture",
      rendu: (compte) => (compte.dateCreation ? formaterDate(compte.dateCreation) : "—"),
    },
    {
      key: "statut",
      entete: "Statut",
      rendu: (compte) => (
        <Badge tone={compte.statutCompte === "ACTIF" ? "success" : compte.statutCompte === "BLOQUE" ? "pending" : "neutral"}>
          {compte.statutCompte}
        </Badge>
      ),
    },
    {
      key: "actions",
      entete: "Action",
      rendu: (compte) => {
        if (compte.statutCompte === "CLOTURE") {
          return <span className="font-body-sm text-body-sm text-neutral-core">Clôturé</span>;
        }

        const geler = compte.statutCompte === "ACTIF";
        return (
          <Bouton
            variant={geler ? "danger" : "outline"}
            disabled={changementStatut.isPending}
            onClick={() =>
              changementStatut.mutate({ id: compte.id, statut: geler ? "BLOQUE" : "ACTIF" })
            }
          >
            {geler ? "Geler" : "Réactiver"}
          </Bouton>
        );
      },
    },
  ];

  return (
    <main className="flex flex-col gap-space-xl px-margin py-space-xl">
      <div>
        <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
          Ora Bank — Comptes bancaires
        </p>
        <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">Comptes bancaires</h1>
        <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
          Ouvrir un compte pour un client existant, geler ou réactiver un compte.
        </p>
      </div>

      <section
        aria-label="Ouvrir un compte"
        className="rounded-lg border border-divider bg-surface-container-lowest p-space-lg"
      >
        <h2 className="font-headline-sm text-headline-sm text-navy">Ouvrir un compte</h2>
        <form onSubmit={soumettre} className="mt-space-md grid gap-space-md md:grid-cols-2 xl:grid-cols-4">
          <ChampSaisie
            label="Identifiant client"
            name="clientId"
            type="number"
            min={1}
            step={1}
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            required
          />
          <div className="flex flex-col gap-space-xs">
            <label htmlFor="typeCompte" className="font-label-md text-label-md text-on-surface">
              Type de compte
            </label>
            <select
              id="typeCompte"
              value={typeCompte}
              onChange={(e) => setTypeCompte(e.target.value as TypeCompte)}
              className="h-10 rounded-lg border border-divider bg-surface-container-lowest px-3 font-body-md text-body-md text-navy focus:border-primary-container focus:outline-2 focus:outline-[rgba(55,111,178,0.2)]"
            >
              {TYPES_COMPTE.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>
          <ChampSaisie
            label="Solde initial (MAD)"
            name="soldeInitial"
            type="number"
            min={0}
            step="0.01"
            value={soldeInitial}
            onChange={(e) => setSoldeInitial(e.target.value)}
            hint="0 par défaut, deux décimales maximum."
          />
          <ChampSaisie
            label="RIB (optionnel)"
            name="rib"
            value={rib}
            onChange={(e) => setRib(e.target.value)}
            hint="Laisser vide pour le générer automatiquement."
            inputMode="numeric"
          />

          <div className="flex items-end md:col-span-2 xl:col-span-4">
            <Bouton type="submit" disabled={ouverture.isPending}>
              {ouverture.isPending ? "Ouverture…" : "Ouvrir le compte"}
            </Bouton>
          </div>
        </form>

        {erreurFormulaire && (
          <p role="alert" className="mt-space-md font-body-sm text-body-sm text-error">
            {erreurFormulaire}
          </p>
        )}
        {succes && (
          <p role="status" className="mt-space-md font-body-sm text-body-sm text-access-teal">
            {succes}
          </p>
        )}
      </section>

      <section aria-label="Liste des comptes" className="flex flex-col gap-space-md">
        <h2 className="font-headline-sm text-headline-sm text-navy">Tous les comptes</h2>

        {comptes.isLoading && (
          <p className="font-body-md text-body-md text-neutral-core">Chargement des comptes…</p>
        )}

        {comptes.isError && (
          <p role="alert" className="font-body-md text-body-md text-error">
            {messageErreur(comptes.error)}
          </p>
        )}

        {comptes.data && (
          <TableauDonnees
            colonnes={colonnes}
            lignes={comptes.data}
            legende="Comptes bancaires de la banque"
            messageVide="Aucun compte bancaire pour le moment."
          />
        )}
      </section>
    </main>
  );
}
