package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(
    @NotBlank(message = "Tên địa điểm không được trống")
    @Size(max = 255, message = "Tên địa điểm không vượt quá 255 ký tự")
    String name,

    String description
) {}
