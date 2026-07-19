package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhan yeu cau chat tu phia client.
 * - message: noi dung tin nhan cua nguoi dung (bat buoc, toi da 2000 ky tu)
 * - sessionId: ID phien hoi thoai (nullable, neu null thi tao phien moi)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotBlank(message = "Noi dung tin nhan khong duoc de trong")
    @Size(max = 2000, message = "Tin nhan khong duoc vuot qua 2000 ky tu")
    private String message;

    /**
     * ID phien hoi thoai. Neu null, he thong se tao session moi.
     * Neu co gia tri, he thong se tiep tuc hoi thoai tu phien cu.
     */
    private String sessionId;
}
