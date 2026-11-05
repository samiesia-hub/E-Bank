package com.SecBank.pro.Services.Clients;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@Service
public class CompteServiceImpl implements CompteService {

    private final ClientRepo clientRepo;
    private final CompteBancaireRepository compteBancaireRepository;

    @Override
    public BigDecimal consulterSolde(Long compteId) {
        CompteBancaire compte = compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec id :" + compteId));
        return compte.getSolde();
    }

    @Override
    public List<CompteBancaire> consulterComptes(Long clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id :" + clientId));
        return compteBancaireRepository.findByClientId(client.getId_Utilisateur());
    }

    @Override
    public void debiter(Long compteId, BigDecimal montant) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void crediter(Long compteId, BigDecimal montant) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void gererComptesBancaires() {
        throw new UnsupportedOperationException("Non implémenté");
    }
}
