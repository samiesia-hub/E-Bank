package com.SecBank.pro.Repositories;

import com.SecBank.pro.Models.RefreshToken;
import com.SecBank.pro.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(Utilisateur user);
}