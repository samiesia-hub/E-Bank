package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Models.Cliens.DossierKYC;

import java.util.List;

/**
 * Validation des dossiers KYC par le support (SCRUM-105 / SCRUM-139).
 */
public interface KycService {

    /** Dossiers en attente, du plus ancien au plus recent. */
    List<DossierKYC> listerDossiersEnAttente();

    /** Passe le dossier a VALIDE et trace la decision dans le journal d'audit. */
    DossierKYC validerDossierKYC(Long dossierId, String emailAgent, String adresseIp);

    /** Passe le dossier a REJETE ; le motif est obligatoire et trace lui aussi. */
    DossierKYC rejeterDossierKYC(Long dossierId, String motif, String emailAgent, String adresseIp);
}
