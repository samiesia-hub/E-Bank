# Plan détaillé fichier par fichier : E-Bank

Ce document dit exactement quel fichier créer, à quel chemin, et qui le crée. Chaque chemin part de la racine du backend :

`pro/src/main/java/com/SecBank/pro/`

Abrégé ci-dessous en `[pro]/`.

Les dossiers existants sont utilisés tels quels :

- `Models/` avec `Admin/` et `Cliens/`
- `Repositories/` avec `Admin/` et `Client/`
- `Services/` avec `Admin/` et `Clients/`
- `Conrollers/` (contrôleurs, à créer plus tard)
- `Configurations/` (sécurité, à créer par le leader)

**Règle de rangement :**

- Ce qui appartient au client va dans `Cliens/` (modèles) ou `Client/` (repositories) ou `Clients/` (services).
- Ce qui appartient au personnel ou à l'administration va dans `Admin/`.
- Ce qui est partagé par tous les acteurs va à la racine du dossier, par exemple `Models/Utilisateur.java`.
- Les énumérations vont dans un nouveau dossier `Models/Enums/`. C'est le seul dossier à créer.

Le nom du fichier doit être exactement le nom de la classe, avec la même casse.

---

## Phase 1 : Modèles

### Personne 1 | Taoufik Mansouri

**Modèles**

| Fichier à créer | Classe |
|---|---|
| `[pro]/Models/Utilisateur.java` | `Utilisateur` (abstraite, avec `@Inheritance`) |
| `[pro]/Models/Cliens/Client.java` | `Client` (hérite de `Utilisateur`) |
| `[pro]/Models/Admin/AgentSupport.java` | `AgentSupport` (hérite de `Utilisateur`) |
| `[pro]/Models/Admin/Administrateur.java` | `Administrateur` (hérite de `Utilisateur`) |
| `[pro]/Models/Admin/Role.java` | `Role` |
| `[pro]/Models/Admin/Permission.java` | `Permission` |
| `[pro]/Models/CodeOTP.java` | `CodeOTP` |
| `[pro]/Models/Cliens/Reclamation.java` | `Reclamation` |
| `[pro]/Models/Cliens/ReponseReclamation.java` | `ReponseReclamation` |
| `[pro]/Models/Admin/TicketSupport.java` | `TicketSupport` |

**Énumérations**

| Fichier à créer | Valeurs |
|---|---|
| `[pro]/Models/Enums/StatutReclamation.java` | `OUVERTE`, `EN_COURS`, `RESOLUE`, `FERMEE` |
| `[pro]/Models/Enums/StatutTicket.java` | `OUVERT`, `EN_TRAITEMENT`, `EN_ATTENTE_CLIENT`, `RESOLU`, `FERME` |
| `[pro]/Models/Enums/Priorite.java` | `BASSE`, `MOYENNE`, `HAUTE`, `CRITIQUE` |

**Repositories**

| Fichier à créer | Interface |
|---|---|
| `[pro]/Repositories/Admin/UtilisateurRepository.java` | `UtilisateurRepository` |
| `[pro]/Repositories/Client/ClientRepository.java` | `ClientRepository` |
| `[pro]/Repositories/Admin/AgentSupportRepository.java` | `AgentSupportRepository` |
| `[pro]/Repositories/Admin/AdministrateurRepository.java` | `AdministrateurRepository` |
| `[pro]/Repositories/Admin/RoleRepository.java` | `RoleRepository` |
| `[pro]/Repositories/Admin/PermissionRepository.java` | `PermissionRepository` |
| `[pro]/Repositories/Admin/CodeOTPRepository.java` | `CodeOTPRepository` |
| `[pro]/Repositories/Client/ReclamationRepository.java` | `ReclamationRepository` |
| `[pro]/Repositories/Client/ReponseReclamationRepository.java` | `ReponseReclamationRepository` |
| `[pro]/Repositories/Admin/TicketSupportRepository.java` | `TicketSupportRepository` |

### Personne 2 | Mohamed Ben Bouaza

**Modèles**

| Fichier à créer | Classe |
|---|---|
| `[pro]/Models/Cliens/CompteBancaire.java` | `CompteBancaire` |
| `[pro]/Models/Cliens/Transaction.java` | `Transaction` (abstraite) |
| `[pro]/Models/Cliens/Virement.java` | `Virement` (hérite de `Transaction`) |
| `[pro]/Models/Cliens/Beneficiaire.java` | `Beneficiaire` |
| `[pro]/Models/Cliens/Releve.java` | `Releve` |
| `[pro]/Models/Cliens/DossierKYC.java` | `DossierKYC` |
| `[pro]/Models/Cliens/DocumentKYC.java` | `DocumentKYC` |
| `[pro]/Models/Admin/JournalAudit.java` | `JournalAudit` |

**Énumérations**

| Fichier à créer | Valeurs |
|---|---|
| `[pro]/Models/Enums/TypeCompte.java` | `COURANT`, `EPARGNE`, `PROFESSIONNEL` |
| `[pro]/Models/Enums/StatutCompte.java` | `ACTIF`, `BLOQUE`, `CLOTURE` |
| `[pro]/Models/Enums/StatutTransaction.java` | `EN_ATTENTE`, `EXECUTEE`, `REJETEE` |
| `[pro]/Models/Enums/StatutKYC.java` | `EN_ATTENTE`, `VALIDE`, `REJETE` |

**Repositories**

| Fichier à créer | Interface |
|---|---|
| `[pro]/Repositories/Client/CompteBancaireRepository.java` | `CompteBancaireRepository` |
| `[pro]/Repositories/Client/TransactionRepository.java` | `TransactionRepository` |
| `[pro]/Repositories/Client/VirementRepository.java` | `VirementRepository` |
| `[pro]/Repositories/Client/BeneficiaireRepository.java` | `BeneficiaireRepository` |
| `[pro]/Repositories/Client/ReleveRepository.java` | `ReleveRepository` |
| `[pro]/Repositories/Client/DossierKYCRepository.java` | `DossierKYCRepository` |
| `[pro]/Repositories/Client/DocumentKYCRepository.java` | `DocumentKYCRepository` |
| `[pro]/Repositories/Admin/JournalAuditRepository.java` | `JournalAuditRepository` |

---

## Phase 2 : Squelettes de méthodes

Chaque squelette est une classe `@Service` avec les méthodes listées. Chaque corps contient uniquement :

```java
throw new UnsupportedOperationException("Non implémenté");
```

Pas de logique, pas d'appel à un repository, pas de méthode en plus.

### Personne 3 | EL GARNI Soukaina

**Fichiers à créer**

| Fichier à créer | Méthodes à déclarer |
|---|---|
| `[pro]/Services/Clients/CompteService.java` | `BigDecimal consulterSolde(Long compteId)`<br>`List<CompteBancaire> consulterComptes(Long clientId)`<br>`void debiter(Long compteId, BigDecimal montant)`<br>`void crediter(Long compteId, BigDecimal montant)`<br>`void gererComptesBancaires()` |
| `[pro]/Services/Clients/VirementService.java` | `Virement effectuerVirement(Long beneficiaireId, BigDecimal montant, String motif)`<br>`void executer(Long virementId)` |
| `[pro]/Services/Clients/BeneficiaireService.java` | `void gererBeneficiaires(Long clientId)` |
| `[pro]/Services/Clients/ReleveService.java` | `Releve telechargerReleve(Long compteId, String periode)` |
| `[pro]/Services/Clients/TransactionService.java` | `List<Transaction> consulterHistorique(Long compteId)`<br>`List<Transaction> consulterTransactions()` |

**Exemple exact, `[pro]/Services/Clients/VirementService.java` :**

```java
package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Virement;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class VirementService {

    public Virement effectuerVirement(Long beneficiaireId, BigDecimal montant, String motif) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    public void executer(Long virementId) {
        throw new UnsupportedOperationException("Non implémenté");
    }
}
```

### Personne 4 | imane batri

**Fichiers à créer**

| Fichier à créer | Méthodes à déclarer |
|---|---|
| `[pro]/Services/Clients/ReclamationService.java` | `Reclamation creerReclamation(Long clientId, String objet, String description)`<br>`Reclamation suivreReclamation(Long reclamationId)`<br>`ReponseReclamation repondreReclamation(Long reclamationId, String contenu)`<br>`Map<String, Object> consulterInfosClient(Long clientId)` |
| `[pro]/Services/Admin/TicketService.java` | `List<TicketSupport> consulterTickets(Long agentId)`<br>`void mettreAJourStatutTicket(Long ticketId, StatutTicket statut)` |
| `[pro]/Services/Admin/AdminService.java` | `void gererUtilisateurs()`<br>`void activerDesactiverCompte(Long utilisateurId)`<br>`void gererRolesEtPermissions()`<br>`void gererEmployesSupport()` |
| `[pro]/Services/Admin/KycService.java` | `void validerDossierKYC(Long dossierId)` |
| `[pro]/Services/Admin/AuditService.java` | `List<JournalAudit> consulterJournauxAudit()` |

---

## Partie du leader : authentification | Sami Es-sadkia

| Fichier à créer | Contenu |
|---|---|
| `[pro]/Services/AuthService.java` | `boolean seConnecter(String email, String motDePasse)`<br>`void seDeconnecter()`<br>`void reinitialiserMotDePasse(String email)`<br>`void gererProfil()` |
| `[pro]/Services/OtpService.java` | `boolean verifier(String code)` |
| `[pro]/Configurations/SecurityConfig.java` | Configuration de sécurité Spring (fichier réel, pas un squelette) |
| `[pro]/Configurations/JwtService.java` | Création et lecture du token JWT (fichier réel) |
| `[pro]/Configurations/JwtAuthFilter.java` | Filtre qui lit le header `Authorization` (fichier réel) |
| `[pro]/Conrollers/AuthController.java` | Endpoints `/api/auth/login`, `/api/auth/register` (fichier réel) |

Les squelettes `AuthService` et `OtpService` sont écrits en premier, avec le même corps `throw` que les autres. Le leader les remplace par le vrai code après.

---

## Ce qui est interdit pendant la phase de squelettes

- Créer un fichier qui n'est pas dans ce document.
- Ajouter une méthode qui n'est pas dans la liste de la personne.
- Écrire une logique métier ou un appel à un repository.
- Modifier un modèle. Toute modification demande un message à l'équipe.
- Déplacer un fichier vers un autre dossier.

---

## Ordre et vérifications

1. **Jour 1 :** le leader fusionne `Utilisateur`, `Role` et `Permission`. Les autres récupèrent `main`.
2. **Jour 1 :** les personnes 1 et 2 créent leurs modèles, énumérations et repositories.
3. **Jour 1 :** les personnes 3 et 4 créent leurs squelettes. Le compilateur peut échouer tant que les modèles ne sont pas fusionnés, c'est normal.
4. **Jour 2 :** les personnes 1 et 2 ouvrent leur PR. Le leader la relit et la fusionne, puis annonce le gel des modèles.
5. **Jour 2 :** les personnes 3 et 4 récupèrent `main`, corrigent les imports, vérifient que le projet compile, puis ouvrent leur PR.

**Vérification avant chaque PR :** le projet compile (`./mvnw compile`) et l'application démarre sans erreur.
