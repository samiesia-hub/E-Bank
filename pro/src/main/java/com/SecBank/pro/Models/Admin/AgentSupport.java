package com.SecBank.pro.Models.Admin;


import com.SecBank.pro.Models.Cliens.Reclamation;
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
public class AgentSupport {

    private String matricule;
    private String specialite;
    @OneToMany(mappedBy = "agentsupp")
    private List<TicketSupport> ticketSupports;
    @OneToMany(mappedBy = "agentsupp")
    private List<Reclamation>reclamations;
}
