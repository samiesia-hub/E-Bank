package com.SecBank.pro.Models;

import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id_Utilisateur;
    private String email;
    @Column(unique = true)
    private String username;
    // Le hash du mot de passe ne sort jamais dans une reponse JSON.
    @JsonIgnore
    private String password;
    private String nom;
    private String CIN;
    private int telephone;
    private boolean actif;
    private boolean doubleauthActive;
    @Enumerated(value = EnumType.STRING)
    private Role role;
    // Boucle JSON possible avec JournalAudit.utilisateur ; non utile dans nos reponses.
    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur")
    private List<JournalAudit> journalAudits;
}
