package com.enterprise.platform.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// 페이지 단위 목록 응답
public class PageResponse<T> {

    private List<T> content;
    private int page;           // 현재 페이지 (0부터 시작)
    private int size;           // 페이지 크기
    private long totalElements; // 전체 개수
    private int totalPages;     // 전체 페이지 수

    public <E> PageResponse(Page<E> page, Function<E, T> mapper) {
        this.content = page.getContent().stream().map(mapper).toList();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}