package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Énums.StatusReclamation;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@AllArgsConstructor
@Service
public class ReclamationServiceImpl implements ReclamationService{
    private final ClientRepo clientRepo;
    private final ReclamationRepo reclamationRepo;
    @Override
    public Reclamation creerReclamation(Long clientId, String object, String description) {
        Client client=clientRepo.findById(clientId).orElseThrow(()->new ResourceNotFoundException("Client introuvable avec id :"+clientId));
        Reclamation reclamation=new Reclamation();
        reclamation.setObjet(object);
        reclamation.setDesciprtion(description);
        reclamation.setDateCreation(new Date());
        reclamation.setStatus(StatusReclamation.OUVERTE);
        reclamation.setClient(client);

        return reclamationRepo.save(reclamation);
    }

    @Override
    public Reclamation suivreReclamation(Long reclamationId) {
        return reclamationRepo.findById(reclamationId).orElseThrow(()->new ResourceNotFoundException("Reclamation introuvable avec id :"+reclamationId));

    }
}
