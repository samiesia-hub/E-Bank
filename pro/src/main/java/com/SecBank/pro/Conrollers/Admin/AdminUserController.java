package com.SecBank.pro.Conrollers.Admin;


import com.SecBank.pro.Dtos.Admin.AdminUserDTO;

import com.SecBank.pro.Models.Enums.Role;

import com.SecBank.pro.Models.UpdateRoleRequest;
import com.SecBank.pro.Services.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration des utilisateurs",
        description = "Supervision des comptes. Réservé au rôle ADMIN.")
public class AdminUserController {

    private final UserServiceImpl userService;

    public AdminUserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @Operation(summary = "Lister les utilisateurs",
            description = "Pagination (`page`, `size`), tri (`sort` parmi id, email, nom, role, actif, "
                    + "dateCreation ; `direction` asc|desc) et filtres optionnels `role` et `actif`.")
    @GetMapping
    public Page<AdminUserDTO> lister(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "id") String tri,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {
        return userService.lister(role, actif, page, size, tri, direction, authentication.getName());
    }

    @Operation(summary = "Détails d'un utilisateur")
    @GetMapping("/{id}")
    public AdminUserDTO details(@PathVariable Long id, Authentication authentication) {
        return userService.details(id, authentication.getName());
    }

    @Operation(summary = "Activer ou suspendre un compte",
            description = "`actif=false` suspend le compte : connexion et refresh refusés (403).")
    @PatchMapping("/{id}/status")
    public AdminUserDTO changerStatut(@PathVariable Long id,
                                      @Valid @RequestBody com.SecBank.pro.Models.UpdateStatusRequest requete,
                                      Authentication authentication) {
        return userService.changerStatut(id, requete.actif(), requete.motif(), authentication.getName());
    }

    @Operation(summary = "Modifier le rôle d'un utilisateur",
            description = "Rôles : ADMIN, CLIENT, AGENT_SUPPORT. Un admin ne peut pas modifier son propre rôle.")
    @PatchMapping("/{id}/role")
    public AdminUserDTO changerRole(@PathVariable Long id,
                                    @Valid @RequestBody UpdateRoleRequest requete,
                                    Authentication authentication) {
        return userService.changerRole(id, requete.role(), authentication.getName());
    }
}