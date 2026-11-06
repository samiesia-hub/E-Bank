package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Utilisateur;

import java.util.List;

public interface AuditService {

    List<JournalAudit> consulterJournauxAudit();

    /**
     * Enregistre une action sensible (operation de guichet, etc.).
     * L'horodatage est genere cote serveur.
     *
     * @return l'entree d'audit persistee
     */
    JournalAudit journaliser(String action, Utilisateur utilisateur, String details);
}
