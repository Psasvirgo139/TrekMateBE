package com.trekmate.backend.service;

import com.trekmate.backend.dto.response.TourCardResponse;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TourService {
    Page<TourCardResponse> getTours(
            String search,
            DifficultyLevel difficulty,
            TourStatus status,
            Short minDuration,
            Short maxDuration,
            Pageable pageable
    );
}
