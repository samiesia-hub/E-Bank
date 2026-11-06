package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepo extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);
}
