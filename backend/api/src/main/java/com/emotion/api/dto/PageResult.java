package com.emotion.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int current;
    private int size;
    private int pages;

    public PageResult(List<T> records, long total, int current, int size, int pages) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = pages;
    }
}