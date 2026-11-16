package com.SecBank.pro.Repositories.Admin;

import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Enums.StatusTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TicketSupportRepo extends JpaRepository<TicketSupport, Long> {

    List<TicketSupport> findAllByOrderByDateOuvertureDesc();

    List<TicketSupport> findByReclamationClientEmailOrderByDateOuvertureDesc(String email);

    /** Nombre de tickets dans l'un des statuts donnes (usage : tickets encore ouverts). */
    long countByStatusIn(Collection<StatusTicket> statuts);
}
