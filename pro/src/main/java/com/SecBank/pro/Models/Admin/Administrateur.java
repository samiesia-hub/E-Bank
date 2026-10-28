package com.SecBank.pro.Models.Admin;


import com.SecBank.pro.Models.Cliens.DossierKYC;
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
public class Administrateur {
    private String matricule;
    private int niveuxAcce;
    @OneToMany(mappedBy = "admin")
    private List<DossierKYC> dossierKYCS;

}
