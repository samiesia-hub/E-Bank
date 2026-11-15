"use client";

// Utilisateurs (admin) — liste, filtres, activation/suspension et rôle via /api/admin/users
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge } from "@/components/interface/Badge";
import { Bouton } from "@/components/interface/Bouton";
import { ChampSaisie } from "@/components/interface/ChampSaisie";
import { TableauDonnees, type DataTableColumn } from "@/components/interface/TableauDonnees";
import { formaterDate } from "@/lib/formatage";
import {
  changerRoleUtilisateur,
  changerStatutUtilisateur,
  listerUtilisateurs,
  messageErreur,
} from "@/services/adminUserService";
import type { RoleUtilisateur, UtilisateurAdmin } from "@/services/adminUserService";

const ROLES: RoleUtilisateur[] = ["ADMIN", "CLIENT", "SUPPORT"];
const TAILLE_PAGE = 20;

type FiltreActif = "tous" | "actifs" | "suspendus";

const CLES_ACTIF: Record<FiltreActif, boolean | undefined> = {
  tous: undefined,
  actifs: true,
  suspendus: false,
};

export default function PageUtilisateurs() {
  const queryClient = useQueryClient();
  const [role, setRole] = useState<RoleUtilisateur | "">("");
  const [filtreActif, setFiltreActif] = useState<FiltreActif>("tous");
  const [recherche, setRecherche] = useState("");
  const [page, setPage] = useState(0);
  const [erreurAction, setErreurAction] = useState<string | null>(null);
  const [succes, setSucces] = useState<string | null>(null);

  const utilisateurs = useQuery({
    queryKey: ["admin", "utilisateurs", role, filtreActif, page],
    queryFn: () =>
      listerUtilisateurs({
        role: role === "" ? undefined : role,
        actif: CLES_ACTIF[filtreActif],
        page,
        size: TAILLE_PAGE,
      }),
  });

  const invalider = () => {
    void queryClient.invalidateQueries({ queryKey: ["admin", "utilisateurs"] });
  };

  const statut = useMutation({
    mutationFn: (variables: { utilisateur: UtilisateurAdmin; actif: boolean }) =>
      changerStatutUtilisateur(variables.utilisateur.id, variables.actif),
    onSuccess: (utilisateur) => {
      setErreurAction(null);
      setSucces(
        utilisateur.actif
          ? `Compte ${utilisateur.email} réactivé.`
          : `Compte ${utilisateur.email} suspendu : ses sessions sont fermées.`
      );
      invalider();
    },
    onError: (erreur) => {
      setSucces(null);
      setErreurAction(messageErreur(erreur));
    },
  });

  const changementRole = useMutation({
    mutationFn: (variables: { utilisateur: UtilisateurAdmin; role: RoleUtilisateur }) =>
      changerRoleUtilisateur(variables.utilisateur.id, variables.role),
    onSuccess: (utilisateur) => {
      setErreurAction(null);
      setSucces(`Rôle de ${utilisateur.email} : ${utilisateur.role}.`);
      invalider();
    },
    onError: (erreur) => {
      setSucces(null);
      setErreurAction(messageErreur(erreur));
    },
  });

  const lignes = (utilisateurs.data?.content ?? []).filter((u) => {
    if (recherche.trim() === "") return true;
    const cible = `${u.email} ${u.nom ?? ""}`.toLowerCase();
    return cible.includes(recherche.trim().toLowerCase());
  });

  const colonnes: DataTableColumn<UtilisateurAdmin>[] = [
    { key: "email", entete: "Email", rendu: (u) => u.email },
    { key: "nom", entete: "Nom", rendu: (u) => u.nom ?? "—" },
    {
      key: "role",
      entete: "Rôle",
      rendu: (u) => (
        <select
          aria-label={`Rôle de ${u.email}`}
          value={u.role}
          onChange={(evenement) =>
            changementRole.mutate({
              utilisateur: u,
              role: evenement.target.value as RoleUtilisateur,
            })
          }
          className="h-9 rounded-lg border border-divider bg-surface-container-lowest px-2 font-body-sm text-body-sm text-navy"
        >
          {ROLES.map((valeur) => (
            <option key={valeur} value={valeur}>
              {valeur}
            </option>
          ))}
        </select>
      ),
    },
    {
      key: "actif",
      entete: "Statut",
      rendu: (u) => (
        <Badge tone={u.actif ? "success" : "error"}>{u.actif ? "Actif" : "Suspendu"}</Badge>
      ),
    },
    {
      key: "dateCreation",
      entete: "Créé le",
      rendu: (u) => (u.dateCreation ? formaterDate(u.dateCreation) : "—"),
    },
    {
      key: "actions",
      entete: "Action",
      rendu: (u) => (
        <Bouton
          variant={u.actif ? "danger" : "outline"}
          disabled={statut.isPending}
          onClick={() => statut.mutate({ utilisateur: u, actif: !u.actif })}
        >
          {u.actif ? "Suspendre" : "Réactiver"}
        </Bouton>
      ),
    },
  ];

  const total = utilisateurs.data?.totalElements ?? 0;
  const pages = utilisateurs.data?.totalPages ?? 1;

  return (
    <main className="mx-auto max-w-[1200px] px-margin py-space-xl">
      <p className="font-label-sm text-label-sm uppercase tracking-wider text-on-surface-variant">
        Orabank — Utilisateurs
      </p>
      <h1 className="mt-space-sm font-headline-md text-headline-md text-navy">Utilisateurs</h1>
      <p className="mt-space-sm max-w-2xl font-body-md text-body-md text-neutral-core">
        {total} compte{total > 1 ? "s" : ""} — activez, suspendez ou changez le rôle d&apos;un utilisateur.
      </p>

      <section className="mt-space-lg flex flex-wrap items-end gap-space-md">
        <ChampSaisie
          label="Rechercher"
          name="recherche"
          placeholder="Email ou nom"
          value={recherche}
          onChange={(evenement) => setRecherche(evenement.target.value)}
        />
        <label className="flex flex-col gap-space-xs">
          <span className="font-label-md text-label-md text-on-surface">Rôle</span>
          <select
            value={role}
            onChange={(evenement) => {
              setPage(0);
              setRole(evenement.target.value as RoleUtilisateur | "");
            }}
            className="h-10 rounded-lg border border-divider bg-surface-container-lowest px-3 font-body-md text-body-md text-navy"
          >
            <option value="">Tous</option>
            {ROLES.map((valeur) => (
              <option key={valeur} value={valeur}>
                {valeur}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-space-xs">
          <span className="font-label-md text-label-md text-on-surface">Statut</span>
          <select
            value={filtreActif}
            onChange={(evenement) => {
              setPage(0);
              setFiltreActif(evenement.target.value as FiltreActif);
            }}
            className="h-10 rounded-lg border border-divider bg-surface-container-lowest px-3 font-body-md text-body-md text-navy"
          >
            <option value="tous">Tous</option>
            <option value="actifs">Actifs</option>
            <option value="suspendus">Suspendus</option>
          </select>
        </label>
      </section>

      {succes && (
        <p role="status" className="mt-space-md font-body-md text-body-md text-access-teal">
          {succes}
        </p>
      )}
      {erreurAction && (
        <p role="alert" className="mt-space-md font-body-md text-body-md text-error">
          {erreurAction}
        </p>
      )}
      {utilisateurs.isError && (
        <p role="alert" className="mt-space-md font-body-md text-body-md text-error">
          {messageErreur(utilisateurs.error)}
        </p>
      )}

      <div className="mt-space-lg">
        {utilisateurs.isLoading ? (
          <p className="font-body-md text-body-md text-neutral-core">Chargement des utilisateurs…</p>
        ) : (
          <TableauDonnees
            colonnes={colonnes}
            lignes={lignes}
            legende="Liste des utilisateurs de la banque"
            messageVide="Aucun utilisateur ne correspond à ces filtres."
          />
        )}
      </div>

      <nav className="mt-space-md flex items-center justify-between" aria-label="Pagination">
        <Bouton
          variant="ghost"
          disabled={page === 0}
          onClick={() => setPage((valeur) => Math.max(0, valeur - 1))}
        >
          ← Précédent
        </Bouton>
        <span className="font-body-sm text-body-sm text-neutral-core">
          Page {page + 1} sur {Math.max(pages, 1)}
        </span>
        <Bouton
          variant="ghost"
          disabled={page + 1 >= pages}
          onClick={() => setPage((valeur) => valeur + 1)}
        >
          Suivant →
        </Bouton>
      </nav>
    </main>
  );
}
