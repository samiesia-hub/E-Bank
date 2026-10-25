package com.SecBank.pro.Repositories;

import com.SecBank.pro.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<Utilisateur,Long> {
}
