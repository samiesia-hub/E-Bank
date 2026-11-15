package com.SecBank.pro.Services;

import com.SecBank.pro.Dtos.Admin.AdminUserDTO;
import com.SecBank.pro.Models.Enums.Role;
import org.springframework.data.domain.Page;

/** Supervision des comptes utilisateurs par un administrateur (SCRUM-97). */
public interface UserService {

    Page<AdminUserDTO> lister(Role role, Boolean actif, int page, int taille,
                              String tri, String direction, String adminEmail);

    AdminUserDTO details(Long id, String adminEmail);

    AdminUserDTO changerStatut(Long id, boolean actif, String motif, String adminEmail);

    AdminUserDTO changerRole(Long id, Role nouveauRole, String adminEmail);
}
