package com.trekmate.backend.service;

import com.trekmate.backend.dto.response.HomeResponse;

public interface HomeService {

    /**
     * Trả về toàn bộ dữ liệu hiển thị trên trang chủ:
     * stats, featured tours, upcoming departures, top guides.
     */
    HomeResponse getHomeData();
}
