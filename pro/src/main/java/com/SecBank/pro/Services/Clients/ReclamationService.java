package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.StatusReclamation;

import java.util.List;

public interface ReclamationService {

    Reclamation creerReclamation(String emailClient, String objet, String message);

    List<Reclamation> listerReclamationsDuClient(String emailClient);

    Reclamation consulterReclamationDuClient(Long reclamationId, String emailClient);

    Reclamation changerStatut(Long reclamationId, StatusReclamation nouveauStatut);
}