package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Beneficiaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeneficiaireRepository
        extends JpaRepository<Beneficiaire,Long> {

    @Query("""
        SELECT b FROM Beneficiaire b
        WHERE b.client.email = :email
        ORDER BY b.nom ASC, b.prenom ASC
    """)
    List<Beneficiaire> trouverParEmailClient(
            @Param("email") String email
    );

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Beneficiaire b
        WHERE b.client.email = :email
          AND b.numeroCompte = :numeroCompte
    """)
    boolean existePourClient(
            @Param("email") String email,
            @Param("numeroCompte") String numeroCompte
    );
}