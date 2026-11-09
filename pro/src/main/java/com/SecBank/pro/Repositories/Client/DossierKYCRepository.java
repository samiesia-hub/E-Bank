package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Enums.StatutKYC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DossierKYCRepository extends JpaRepository<DossierKYC, Long> {

    List<DossierKYC> findByStatutKYCOrderByDateSoumissionAsc(StatutKYC statut);
}
