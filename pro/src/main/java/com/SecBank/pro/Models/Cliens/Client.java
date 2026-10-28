package com.SecBank.pro.Models.Cliens;


import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@PrimaryKeyJoinColumn(name="user_id")
public class Client {
private String numeroClient;
private String address;
private Date dateNaissance;
@OneToMany(mappedBy = "client")
private List<Reclamation> reclamations;
@OneToMany(mappedBy = "client")
private List<CompteBancaire> compteBancaires;
@OneToMany(mappedBy = "client")
    private List<Beneficiaire> beneficiaires;
@OneToOne(mappedBy = "client_id")
    private DossierKYC dossierKYC;
}
