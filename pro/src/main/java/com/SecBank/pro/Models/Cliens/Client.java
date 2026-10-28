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
    @OneToMany(mappedBy = "client")
    private List<Reclamation> reclamations;
    @OneToMany(mappedBy = "client")
    private List<CompteBancaire> compteBancaires;
    @OneToMany(mappedBy = "client")
    private List<Beneficiaire> beneficiaires;
    @OneToOne(mappedBy = "client")
    private DossierKYC dossierKYC;
}
