package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {

    @Query("SELECT c FROM CompteBancaire c WHERE c.client.id_Utilisateur = :clientId")
    List<CompteBancaire> findByClientId(@Param("clientId") Long clientId);

    boolean existsByNumeroCompte(String numeroCompte);

    boolean existsByRib(String rib);

    /**
     * Mise a jour atomique du solde cote base (aucun ecrasement concurrent).
     * Le compte doit exister et etre ACTIF.
     *
     * @return 1 si le solde a ete augmente, 0 sinon
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompteBancaire c SET c.solde = c.solde + :montant "
            + "WHERE c.id = :compteId AND c.statutCompte = :statut")
    int incrementerSolde(@Param("compteId") Long compteId,
                         @Param("montant") BigDecimal montant,
                         @Param("statut") StatutCompte statut);

    /**
     * Mise a jour atomique du solde : la condition solde >= montant est evaluee
     * dans la meme instruction, deux retraits concurrents ne peuvent donc pas
     * faire passer le solde sous zero.
     *
     * @return 1 si le solde a ete diminue, 0 sinon (solde insuffisant ou compte inoperable)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompteBancaire c SET c.solde = c.solde - :montant "
            + "WHERE c.id = :compteId AND c.statutCompte = :statut AND c.solde >= :montant")
    int decrementerSoldeSiSoldeSuffisant(@Param("compteId") Long compteId,
                                         @Param("montant") BigDecimal montant,
                                         @Param("statut") StatutCompte statut);
}
