package com.SecBank.pro.Services;

import com.SecBank.pro.Dtos.Admin.AdminUserDTO;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.Admin.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    /** Liste blanche : on ne trie jamais sur un champ sensible (ex. motDePasse). */
    private static final Set<String> CHAMPS_TRI =
            Set.of("id", "email", "nom", "role", "actif", "dateCreation");
    private static final int TAILLE_MAX = 100;

    private final UserRepo userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Override
    @Transactional
    public Page<AdminUserDTO> lister(Role role, Boolean actif, int page, int taille,
                                     String tri, String direction, String adminEmail) {
        if (!CHAMPS_TRI.contains(tri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tri non autorisé. Champs possibles : " + CHAMPS_TRI);
        }
        boolean descendant = "desc".equalsIgnoreCase(direction);
        // Spring Data decoupe les noms de propriete sur "_" : le tri est donc pose
        // directement dans la specification, sur le champ reel de l'entite.
        String propriete = "id".equals(tri) ? "id_Utilisateur" : tri;
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(taille, 1), TAILLE_MAX));

        Specification<Utilisateur> filtre = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (role != null) {
                predicats.add(cb.equal(root.get("role"), role));
            }
            if (actif != null) {
                predicats.add(cb.equal(root.get("actif"), actif));
            }
            // La requete de comptage ne porte pas de tri.
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.orderBy(descendant ? cb.desc(root.get(propriete)) : cb.asc(root.get(propriete)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };

        Page<AdminUserDTO> resultat = userRepository.findAll(filtre, pageable)
                .map(AdminUserDTO::depuis);

        journaliser(adminEmail, "UTILISATEURS_CONSULTES",
                "role=" + role + ", actif=" + actif + ", page=" + pageable.getPageNumber()
                        + ", taille=" + pageable.getPageSize());
        return resultat;
    }

    @Override
    @Transactional
    public AdminUserDTO details(Long id, String adminEmail) {
        Utilisateur utilisateur = trouver(id);
        journaliser(adminEmail, "UTILISATEUR_CONSULTE", "utilisateur#" + id);
        return AdminUserDTO.depuis(utilisateur);
    }

    @Override
    @Transactional
    public AdminUserDTO changerStatut(Long id, boolean actif, String motif, String adminEmail) {
        Utilisateur utilisateur = trouver(id);

        // Un admin ne peut pas se suspendre lui-meme : il reste toujours au moins
        // un administrateur actif.
        if (!actif && estMemeCompte(utilisateur, adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas suspendre votre propre compte");
        }
        if (utilisateur.isActif() == actif) {
            return AdminUserDTO.depuis(utilisateur); // rien a changer
        }

        utilisateur.setActif(actif);
        userRepository.save(utilisateur);

        if (!actif) {
            // Empeche de rouvrir une session via un refresh token existant.
            refreshTokenService.revokeAllByUser(utilisateur);
        }

        journaliser(adminEmail,
                actif ? "UTILISATEUR_ACTIVE" : "UTILISATEUR_SUSPENDU",
                "utilisateur#" + id + ", email=" + utilisateur.getEmail()
                        + ", motif=" + (motif == null ? "" : motif));
        return AdminUserDTO.depuis(utilisateur);
    }

    @Override
    @Transactional
    public AdminUserDTO changerRole(Long id, Role nouveauRole, String adminEmail) {
        Utilisateur utilisateur = trouver(id);

        if (estMemeCompte(utilisateur, adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas modifier votre propre rôle");
        }
        Role ancienRole = utilisateur.getRole();
        if (ancienRole == nouveauRole) {
            return AdminUserDTO.depuis(utilisateur); // rien a changer
        }

        utilisateur.setRole(nouveauRole);
        userRepository.save(utilisateur);

        journaliser(adminEmail, "ROLE_MODIFIE",
                "utilisateur#" + id + ", email=" + utilisateur.getEmail()
                        + ", " + ancienRole + " -> " + nouveauRole);
        return AdminUserDTO.depuis(utilisateur);
    }

    // ------------------------------------------------------------------

    private Utilisateur trouver(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private static boolean estMemeCompte(Utilisateur utilisateur, String adminEmail) {
        return adminEmail != null && adminEmail.equalsIgnoreCase(utilisateur.getEmail());
    }

    /**
     * Trace l'action dans le journal d'audit. L'auteur est retrouve par son email ;
     * s'il est inconnu l'entree est tout de meme ecrite, sans auteur.
     */
    private void journaliser(String adminEmail, String action, String details) {
        Utilisateur auteur = adminEmail == null
                ? null
                : userRepository.findByEmail(adminEmail).orElse(null);
        auditService.journaliser(action, auteur, details);
    }
}
