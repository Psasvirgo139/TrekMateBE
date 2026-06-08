package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;

public record TourSearchRequest(
        String search,
        DifficultyLevel difficulty,
        TourStatus status,
        Integer page,
        Integer size,
        String sortBy,
        String direction
) {
    public int getPageNumber() {
        return page != null ? page : 0;
    }

    public int getPageSize() {
        return size != null ? size : 10;
    }

    public String getSortField() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String getSortDirection() {
        return direction != null ? direction : "DESC";
    }
}
