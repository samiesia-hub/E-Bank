package com.SecBank.pro.Services.Admin;

import java.util.List;
import com.SecBank.pro.Models.Admin.JournalAudit;

public interface AuditService {
    List<JournalAudit> consulterJournauxAudit();
}