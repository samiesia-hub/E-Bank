package com.SecBank.pro.Models.Admin;

import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Énums.Priorite;
import com.SecBank.pro.Models.Énums.StatusTicket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TicketSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private StatusTicket status;
    @Enumerated(EnumType.STRING)
    private Priorite priorite;
    private Date dateOuverture ;
    private Date dateFermetur;
    @ManyToOne
    @JoinColumn(name = "agent_id")
    private AgentSupport agentsupp;
    @OneToOne
    @JoinColumn(name = "reclamation_id")
    private Reclamation reclamation;
}
