package com.SecBank.pro.Models.Cliens;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "transaction_id")
public class Virement extends Transaction {

    @Column(unique = true, length = 40)
    private String reference;

    private String beneficiaire;
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destinataire_id")
    private CompteBancaire compteDestinataire;
}