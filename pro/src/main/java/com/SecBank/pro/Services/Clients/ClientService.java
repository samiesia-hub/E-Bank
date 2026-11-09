package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Dtos.ChangePasswordRequestDTO;
import com.SecBank.pro.Dtos.ModifierProfilRequest;
import com.SecBank.pro.Models.Cliens.Client;

public interface ClientService {

    Client consulterProfil(String email);

    Client modifierProfil(String email, ModifierProfilRequest requete);

    void changerMotDePasse(String email, ChangePasswordRequestDTO requete);
}