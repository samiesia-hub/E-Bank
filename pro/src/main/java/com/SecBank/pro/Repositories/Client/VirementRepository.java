package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Virement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VirementRepository extends JpaRepository<Virement, Long> {

    Optional<Virement> findByReference(String reference);
}