package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.TourAttributeRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.model.TourAttribute;
import com.trekmate.backend.model.enums.TourAttributeType;
import com.trekmate.backend.repository.TourAttributeRepository;
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
@Slf4j(topic = "API-TOUR-ATTRIBUTE-CONTROLLER")
@RequestMapping("/tour-attributes")
@RequiredArgsConstructor
@Tag(name = "Tour Attributes", description = "Quản lý và tra cứu thông tin điểm nhấn, dịch vụ bao gồm/loại trừ, và các yêu cầu của tour")
public class TourAttributeController {

    private final TourAttributeRepository tourAttributeRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách thuộc tính tour", description = "Trả về danh sách thuộc tính theo loại (type) và từ khóa tìm kiếm (search).")
    public ApiResponse<List<TourAttribute>> getTourAttributes(
            @RequestParam TourAttributeType type,
            @RequestParam(required = false) String search) {
        log.info("REST request to search tour attributes: type='{}', search='{}'", type, search);
        List<TourAttribute> data = tourAttributeRepository.searchByTypeAndContent(type, search);
        return ApiResponse.<List<TourAttribute>>builder()
                .code(200)
                .message("Lấy danh sách thuộc tính tour thành công")
                .data(data)
                .build();
    }

    @PostMapping
    @Operation(summary = "Thêm thuộc tính tour mới", description = "Lưu thuộc tính mới (highlights, includes, excludes, requirements) vào Database.")
    public ResponseEntity<TourAttribute> createTourAttribute(@Valid @RequestBody TourAttributeRequest request) {
        log.info("REST request to create tour attribute: {}", request);
        
        // Tránh trùng lặp: nếu đã tồn tại, trả về bản ghi hiện tại
        return tourAttributeRepository.findByTypeAndContentIgnoreCase(request.type(), request.content().trim())
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    TourAttribute newAttr = TourAttribute.builder()
                            .content(request.content().trim())
                            .type(request.type())
                            .build();
                    TourAttribute saved = tourAttributeRepository.save(newAttr);
                    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
                });
    }
}
