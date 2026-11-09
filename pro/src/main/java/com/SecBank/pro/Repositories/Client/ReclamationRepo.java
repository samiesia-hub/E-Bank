package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepo extends JpaRepository<Reclamation,Long> {

    // "ClientEmail" = champ email du client (hérité d'Utilisateur) ; tri du plus récent au plus ancien
    List<Reclamation> findByClientEmailOrderByDateCreationDesc(String email);
}