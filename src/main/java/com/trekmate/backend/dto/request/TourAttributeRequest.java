package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.TourAttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TourAttributeRequest(
    @NotBlank(message = "Nội dung thuộc tính không được trống")
    @Size(max = 1000, message = "Nội dung thuộc tính không vượt quá 1000 ký tự")
    String content,

    @NotNull(message = "Loại thuộc tính bắt buộc phải chọn")
    TourAttributeType type
) {}
