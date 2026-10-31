package com.SecBank.pro.Services;

import com.SecBank.pro.Models.Cliens.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Override
    public List<Transaction> consulterHistorique(Long compteId) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public List<Transaction> consulterTransactions() {
        throw new UnsupportedOperationException("Non implémenté");
    }
}