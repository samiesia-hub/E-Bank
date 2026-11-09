package com.SecBank.pro.Repositories.Admin;

import com.SecBank.pro.Models.Admin.TicketSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSupportRepo extends JpaRepository<TicketSupport, Long> {

    List<TicketSupport> findAllByOrderByDateOuvertureDesc();

    List<TicketSupport> findByReclamationClientEmailOrderByDateOuvertureDesc(String email);
}
