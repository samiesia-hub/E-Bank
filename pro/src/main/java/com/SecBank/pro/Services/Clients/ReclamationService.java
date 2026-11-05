package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Reclamation;

public interface ReclamationService {
    Reclamation creerReclamation(Long clientId,String object,String description);
    Reclamation suivreReclamation(Long reclamationId);
}
