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
import org.springframework.data.domain.Sort;
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
    @Transactional(readOnly = true)
    public Page<AdminUserDTO> lister(Role role, Boolean actif, int page, int taille,
                                     String tri, String direction, String adminEmail) {
        if (!CHAMPS_TRI.contains(tri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tri non autorisé. Champs possibles : " + CHAMPS_TRI);
        }
        Sort.Direction sens = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(taille, 1), TAILLE_MAX),
                Sort.by(sens, tri));

        Specification<Utilisateur> filtre = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (role != null) {
                predicats.add(cb.equal(root.get("role"), role));
            }
            if (actif != null) {
                predicats.add(cb.equal(root.get("actif"), actif));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };

        Page<AdminUserDTO> resultat = userRepository.findAll(filtre, pageable)
                .map(AdminUserDTO::depuis);

        auditService.enregistrer(adminEmail, "UTILISATEURS_CONSULTES", "liste",
                "role=" + role + ", actif=" + actif + ", page=" + pageable.getPageNumber()
                        + ", taille=" + pageable.getPageSize());
        return resultat;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDTO details(Long id, String adminEmail) {
        Utilisateur utilisateur = trouver(id);
        auditService.enregistrer(adminEmail, "UTILISATEUR_CONSULTE", cible(id), "");
        return AdminUserDTO.depuis(utilisateur);
    }

    @Override
    @Transactional
    public AdminUserDTO changerStatut(Long id, boolean actif, String motif, String adminEmail) {
        Utilisateur utilisateur = trouver(id);

        // Un admin actif ne peut pas se suspendre : cela garantit aussi
        // qu'il reste toujours au moins un administrateur actif.
        if (!actif && estMemeCompte(utilisateur, adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas suspendre votre propre compte");
        }
        if (utilisateur.isActif() == actif) {
            return AdminUserDTO.depuis(utilisateur); // rien à changer
        }

        utilisateur.setActif(actif);
        userRepository.save(utilisateur);

        if (!actif) {
            // Empêche de rouvrir une session via un refresh token existant.
            refreshTokenService.revokeAllByUser(utilisateur);
        }

        auditService.enregistrer(adminEmail,
                actif ? "UTILISATEUR_ACTIVE" : "UTILISATEUR_SUSPENDU",
                cible(id),
                "email=" + utilisateur.getEmail() + ", motif=" + (motif == null ? "" : motif));
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
            return AdminUserDTO.depuis(utilisateur); // rien à changer
        }

        utilisateur.setRole(nouveauRole);
        userRepository.save(utilisateur);

        auditService.enregistrer(adminEmail, "ROLE_MODIFIE", cible(id),
                "email=" + utilisateur.getEmail() + ", " + ancienRole + " -> " + nouveauRole);
        return AdminUserDTO.depuis(utilisateur);
    }

    // ------------------------------------------------------------------

    private Utilisateur trouver(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private static boolean estMemeCompte(Utilisateur utilisateur, String adminEmail) {
        return utilisateur.getEmail().equalsIgnoreCase(adminEmail);
    }

    private static String cible(Long id) {
        return "utilisateur#" + id;
    }
}