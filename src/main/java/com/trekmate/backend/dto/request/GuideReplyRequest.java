package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideReplyRequest {

    @NotBlank(message = "Reply content is required")
    @Size(max = 2000, message = "Reply must be at most 2000 characters")
    private String guideReply;
}
