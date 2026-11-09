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

    /**
     * Variante utilisee par les actions exposees sur HTTP : l'adresse IP de
     * l'appelant est ajoutee aux details. L'entite JournalAudit n'a pas encore
     * de colonne dediee (a ajouter avec l'accord de l'equipe, cf. SCRUM-105).
     */
    JournalAudit journaliser(String action, Utilisateur utilisateur, String details, String adresseIp);
}
