package com.SecBank.pro.Models;

import com.SecBank.pro.Models.Énums.Role;
import jakarta.persistence.*;
import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String password;
    private String nom;
    private String CIN;
    private int telephone;
    private boolean actif;
    private boolean doubleauthActive;
    @Enumerated(value = EnumType.STRING)
    private Role role;
}
