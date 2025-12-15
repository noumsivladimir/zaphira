package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Réponse générique pour les listes paginées
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    @JsonProperty("content")
    private List<T> content;

    @JsonProperty("totalElements")
    private Long totalElements;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("currentPage")
    private Integer currentPage;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("hasNextPage")
    private Boolean hasNextPage;

    @JsonProperty("hasPreviousPage")
    private Boolean hasPreviousPage;

    public static <T> PagedResponse<T> of(List<T> content, Long totalElements) {
        return PagedResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .build();
    }
}
