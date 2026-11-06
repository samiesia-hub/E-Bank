package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Enums.TypeCompte;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consultation et creation des comptes bancaires.
 * Tous les soldes sont manipules en BigDecimal (jamais de double/float).
 */
public interface CompteService {

    /** Devise attribuee par defaut aux nouveaux comptes (ISO 4217). */
    String DEVISE_PAR_DEFAUT = "MAD";

    BigDecimal consulterSolde(Long compteId);

    List<CompteBancaire> consulterComptes(Long clientId);

    /** Comptes du client authentifie, resolu par l'email porte par le token. */
    List<CompteBancaire> consulterComptesDuClient(String email);

    /** Details d'un compte, uniquement si l'email est celui du proprietaire. */
    CompteBancaire consulterCompteDuClient(Long compteId, String email);

    /** Details d'un compte sans filtre d'appartenance (reserve a l'administration). */
    CompteBancaire consulterCompte(Long compteId);

    /** Cree un compte ACTIF, en MAD, solde 0.00, avec numero de compte et RIB generes uniques. */
    CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte);

    /**
     * Cree un compte avec un RIB eventuellement saisi par l'agent.
     * RIB absent : numero de compte et RIB sont generes.
     * RIB fourni : format 24 chiffres et cle de controle verifies (400 avec le motif),
     * unicite verifiee en base (409 si deja attribue).
     */
    CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte, String rib);

    /** Debit atomique du solde ; 400 si montant invalide ou solde insuffisant. */
    void debiter(Long compteId, BigDecimal montant);

    /** Credit atomique du solde ; 400 si montant invalide. */
    void crediter(Long compteId, BigDecimal montant);

    /**
     * Depot d'especes au guichet : solde augmente atomiquement, transaction DEPOT
     * creee avec l'agent auteur et le motif, action journalisee dans l'audit.
     */
    OperationGuichet deposer(Long compteId, BigDecimal montant, String motif, String emailAgent);

    /**
     * Retrait d'especes au guichet : refuse (400) si le solde est insuffisant,
     * sinon solde diminue atomiquement, transaction RETRAIT tracee et auditee.
     */
    OperationGuichet retirer(Long compteId, BigDecimal montant, String motif, String emailAgent);

    void gererComptesBancaires();
}
