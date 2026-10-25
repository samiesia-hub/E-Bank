package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepo extends JpaRepository<Client,Long> {
}
