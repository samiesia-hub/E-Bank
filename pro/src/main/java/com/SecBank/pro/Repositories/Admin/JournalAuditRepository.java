package com.SecBank.pro.Repositories.Admin;

import com.SecBank.pro.Models.Admin.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    /** Dernieres entrees du journal, les plus recentes d'abord. */
    List<JournalAudit> findTop5ByOrderByDateActionDesc();
}
