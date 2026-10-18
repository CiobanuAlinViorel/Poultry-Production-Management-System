package com.ppms.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public class PageRequestParams {

    private static final int MAX_SIZE = 100;

    public static PageRequestParams of(int page, int size, String sort, Set<String> allowedFields) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        Sort resolved = parseSort(sort, allowedFields);
        return new PageRequestParams(safePage, safeSize, resolved, sort);
    }

    private final int page;
    private final int size;
    private final Sort sort;
    private final String rawSort;

    private PageRequestParams(int page, int size, Sort sort, String rawSort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
        this.rawSort = rawSort;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public Sort sort() {
        return sort;
    }

    public String rawSort() {
        return rawSort;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, sort);
    }

    private static Sort parseSort(String sort, Set<String> allowedFields) {
        if (sort == null || sort.isBlank() || allowedFields == null || allowedFields.isEmpty()) {
            return Sort.unsorted();
        }
        String[] parts = sort.split(",");
        if (parts.length < 2) {
            return Sort.unsorted();
        }
        String field = parts[0].trim();
        String direction = parts[1].trim().toUpperCase();
        if (!allowedFields.contains(field)) {
            return Sort.unsorted();
        }
        return Sort.by(Sort.Direction.fromString(direction), field);
    }
}
