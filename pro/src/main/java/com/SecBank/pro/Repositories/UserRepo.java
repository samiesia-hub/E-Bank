package com.SecBank.pro.Repositories;

import com.SecBank.pro.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<Utilisateur,Long> {

    Optional<Utilisateur> findByEmail(String email);
}
