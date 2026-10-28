package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Énums.StatutKYC;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DossierKYC {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private StatutKYC statutKYC;
    private LocalDateTime dateSoumission;

    @OneToOne
    @JoinColumn(name = "client_id",unique = true,nullable = false)
    private Client client;
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Administrateur admin;
}
