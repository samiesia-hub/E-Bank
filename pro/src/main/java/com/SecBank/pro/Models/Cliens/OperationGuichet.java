package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Enums.TypeOperation;
import com.SecBank.pro.Models.Utilisateur;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Depot ou retrait d'especes effectue au guichet par un agent.
 * L'agent auteur est conserve sur la transaction pour la tracabilite.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "transaction_id")
public class OperationGuichet extends Transaction {

    @Enumerated(EnumType.STRING)
    private TypeOperation typeOperation;

    private String motif;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Utilisateur agent;
}
