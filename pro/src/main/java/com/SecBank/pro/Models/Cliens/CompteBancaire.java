package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Énums.StatutCompte;
import com.SecBank.pro.Models.Énums.TypeCompte;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    @JoinColumn(name = "client_id")
    private Client client;
    @OneToMany(mappedBy = "compte")
    private List<Transaction> transactions;
    @OneToMany(mappedBy = "compte")
    private List<Releve> releves;
}
