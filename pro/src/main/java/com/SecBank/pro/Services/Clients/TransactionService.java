package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Transaction;

import java.util.List;

public interface TransactionService {

    List<Transaction> consulterHistorique(Long compteId);

    List<Transaction> consulterTransactions();
}
