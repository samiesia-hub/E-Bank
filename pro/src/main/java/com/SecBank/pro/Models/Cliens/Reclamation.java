package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Admin.AgentSupport;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Énums.StatusReclamation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String Objet;
    private String desciprtion;
    private Date dateCreation;
    @Enumerated(value = EnumType.STRING)
    private StatusReclamation status;
    @ManyToOne
    @JoinColumn(name = "agent_id")
    private AgentSupport agentsupp;
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @OneToOne(mappedBy = "reclamation")
    private TicketSupport ticketSupport;
    @OneToMany(mappedBy = "reclamation")
    private List<ResponseReclamation> responseReclamations;


}
