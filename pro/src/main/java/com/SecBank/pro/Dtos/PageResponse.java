package com.SecBank.pro.Dtos;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination stable pour l'API (au lieu de serialiser PageImpl).
 */
public record PageResponse<T>(
        List<T> contenu,
        int page,
        int taille,
        long totalElements,
        int totalPages,
        boolean dernierePage
) {
    public static <T> PageResponse<T> depuis(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
