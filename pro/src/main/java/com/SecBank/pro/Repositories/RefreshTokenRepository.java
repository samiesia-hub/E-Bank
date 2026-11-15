package com.SecBank.pro.Repositories;

import com.SecBank.pro.Models.RefreshToken;
import com.SecBank.pro.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(Utilisateur user);

    List<RefreshToken> findAllByUserAndRevokedFalse(Utilisateur user);
}
