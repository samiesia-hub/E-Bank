package com.SecBank.pro.Services.Clients;

<<<<<<< HEAD
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
=======
import com.SecBank.pro.Models.Cliens.Reclamation;

public interface ReclamationService {
    Reclamation creerReclamation(Long clientId,String object,String description);
    Reclamation suivreReclamation(Long reclamationId);
}
>>>>>>> 3d4f4bc (j'ai mes tache de spring 1)
