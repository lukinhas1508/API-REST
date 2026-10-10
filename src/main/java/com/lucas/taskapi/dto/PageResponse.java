package com.lucas.taskapi.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Resposta paginada própria, para não expor a estrutura interna do Page do Spring na API.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
