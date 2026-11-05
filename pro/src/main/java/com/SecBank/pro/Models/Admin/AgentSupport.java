package com.SecBank.pro.Models.Admin;


import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Utilisateur;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@PrimaryKeyJoinColumn(name="user_id")
public class AgentSupport extends Utilisateur {

    private String matricule;
    private String specialite;
    // Boucles JSON possibles avec TicketSupport et Reclamation : on casse la boucle.
    @JsonIgnore
    @OneToMany(mappedBy = "agentsupp")
    private List<TicketSupport> ticketSupports;
    @JsonIgnore
    @OneToMany(mappedBy = "agentsupp")
    private List<Reclamation>reclamations;
}
