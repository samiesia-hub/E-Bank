package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.CompteBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {

    @Query("SELECT c FROM CompteBancaire c WHERE c.client.id_Utilisateur = :clientId")
    List<CompteBancaire> findByClientId(@Param("clientId") Long clientId);
}
