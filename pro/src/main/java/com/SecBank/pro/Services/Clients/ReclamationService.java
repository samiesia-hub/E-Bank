package com.SecBank.pro.Services.Clients;

import java.util.Map;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Cliens.ResponseReclamation;

public interface ReclamationService {

    Reclamation creerReclamation(
        Long clientId,
        String objet,
        String description
    );

    Reclamation suivreReclamation(Long reclamationId);

    ResponseReclamation repondreReclamation(
        Long reclamationId,
        String contenu
    );

    Map<String, Object> consulterInfosClient(Long clientId);
}