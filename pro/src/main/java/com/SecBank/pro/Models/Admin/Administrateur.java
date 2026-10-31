package com.SecBank.pro.Models.Admin;


import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Utilisateur;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@PrimaryKeyJoinColumn(name="user_id")
<<<<<<< HEAD
public class Administrateur {
=======
<<<<<<< HEAD
public class Administrateur extends Utilisateur{
=======
public class Administrateur extends Utilisateur {
>>>>>>> 3d4f4bc (j'ai mes tache de spring 1)
>>>>>>> 47ea36a (les tache de spring 1 par Taoufik sont bien fait)
    private String matricule;
    private int niveuxAcce;
    @OneToMany(mappedBy = "admin")
    private List<DossierKYC> dossierKYCS;

}
