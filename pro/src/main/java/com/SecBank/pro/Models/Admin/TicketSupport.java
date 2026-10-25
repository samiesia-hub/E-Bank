package com.SecBank.pro.Models.Admin;

import com.SecBank.pro.Models.Énums.Priorite;
import com.SecBank.pro.Models.Énums.StatusTicket;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private StatusTicket status;
    private Priorite priorite;
    private Date dateOuverture ;
    private Date dateFermetur;
}
