package com.SecBank.pro.Repositories;

import com.SecBank.pro.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepo extends JpaRepository<Utilisateur,Long>, JpaSpecificationExecutor<Utilisateur> {

    Optional<Utilisateur> findByEmail(String email);

    /** Utilisateurs dont le compte n'est pas desactive. */
    long countByActifTrue();
}
