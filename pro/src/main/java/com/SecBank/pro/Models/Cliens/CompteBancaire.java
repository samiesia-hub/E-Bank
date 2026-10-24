package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Énums.StatutCompte;
import com.SecBank.pro.Models.Énums.TypeCompte;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteBancaire {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String numeroCompte;
    private BigDecimal solde;
    @Enumerated(EnumType.STRING)
    private TypeCompte typeCompte;
    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte;
    private LocalDateTime dateCreation;

    @ManyToOne
    private Client client;
}
