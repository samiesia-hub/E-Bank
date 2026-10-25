package com.SecBank.pro.Repositories.Admin;

import com.SecBank.pro.Models.Admin.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepo extends JpaRepository<Administrateur,Long> {
}
