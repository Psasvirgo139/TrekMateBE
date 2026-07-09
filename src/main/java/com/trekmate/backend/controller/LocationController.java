package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.LocationRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.model.Location;
import com.trekmate.backend.repository.LocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@Slf4j(topic = "API-LOCATION-CONTROLLER")
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Quản lý và tra cứu thông tin địa điểm")
public class LocationController {

    private final LocationRepository locationRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách địa điểm", description = "Trả về danh sách địa điểm theo từ khóa tìm kiếm (không phân biệt chữ hoa thường).")
    public ApiResponse<List<Location>> getLocations(@RequestParam(required = false) String search) {
        log.info("REST request to search locations: search='{}'", search);
        List<Location> data = locationRepository.searchByName(search);
        return ApiResponse.<List<Location>>builder()
                .code(200)
                .message("Lấy danh sách địa điểm thành công")
                .data(data)
                .build();
    }

    @PostMapping
    @Operation(summary = "Thêm địa điểm mới", description = "Lưu địa điểm mới vào Database.")
    public ResponseEntity<Location> createLocation(@Valid @RequestBody LocationRequest request) {
        log.info("REST request to create location: {}", request);
        
        // Kiểm tra xem địa điểm đã tồn tại chưa (không phân biệt chữ hoa thường)
        // Nếu đã tồn tại, trả về địa điểm đó để tránh duplicate và lỗi constraint
        return locationRepository.searchByName(request.name().trim()).stream()
                .filter(l -> l.getName().equalsIgnoreCase(request.name().trim()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Location location = Location.builder()
                            .name(request.name().trim())
                            .description(request.description())
                            .build();
                    Location saved = locationRepository.save(location);
                    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
                });
    }
}
