package com.SecBank.pro.Models.Cliens;


import com.SecBank.pro.Models.Utilisateur;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@PrimaryKeyJoinColumn(name="user_id")
public class Client extends Utilisateur {
    private String numeroClient;
    private String address;
    private Date dateNaissance;
    // Boucle JSON infinie reclamation -> client -> reclamations : on casse la boucle.
    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<Reclamation> reclamations;
    // Meme raison : sans cela reclamation -> client -> comptes -> client -> ...
    // depasse la profondeur JSON maximale et le corps de la reponse est perdu.
    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<CompteBancaire> compteBancaires;
    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<Beneficiaire> beneficiaires;
    @JsonIgnore
    @OneToOne(mappedBy = "client")
    private DossierKYC dossierKYC;
}
