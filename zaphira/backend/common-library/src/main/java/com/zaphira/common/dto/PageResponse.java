package com.zaphira.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response wrapper
 * 
 * @param <T> The type of items in the page
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    
    /**
     * The items in the current page
     */
    private List<T> content;
    
    /**
     * Current page number (0-indexed)
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int size;
    
    /**
     * Total number of items across all pages
     */
    private long totalElements;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Whether this is the first page
     */
    private boolean first;
    
    /**
     * Whether this is the last page
     */
    private boolean last;
    
    /**
     * Whether there is a next page
     */
    private boolean hasNext;
    
    /**
     * Whether there is a previous page
     */
    private boolean hasPrevious;
    
    /**
     * Create PageResponse from Spring Data Page
     * 
     * @param page Spring Data Page object
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
    
    /**
     * Create PageResponse from Spring Data Page with mapped content
     * 
     * @param page Spring Data Page object
     * @param mapper Function to map content type
     * @return PageResponse with mapped content
     */
    public static <T, U> PageResponse<U> of(Page<T> page, java.util.function.Function<T, U> mapper) {
        return PageResponse.<U>builder()
            .content(page.getContent().stream().map(mapper).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
}
