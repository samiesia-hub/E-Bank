package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Énums.StatutTransaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private LocalDateTime dateTransaction;
    private BigDecimal montant;
    @Enumerated(EnumType.STRING)
    private StatutTransaction statutTransaction;

    @ManyToOne
    private CompteBancaire compte;
}
