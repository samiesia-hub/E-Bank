package com.SecBank.pro.Models.Cliens;

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
    private Client client;
}
