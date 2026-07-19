package com.trekmate.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tra ve ket qua tu AI chat.
 * - answer: cau tra loi cua AI (dinh dang Markdown)
 * - sessionId: ID phien hoi thoai (de client gui lai trong cac request tiep theo)
 * - suggestions: danh sach cau hoi goi y nhanh de nguoi dung co the bam chon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    /** Cau tra loi cua AI, co the chua Markdown formatting */
    private String answer;

    /** ID phien hoi thoai hien tai */
    private String sessionId;

    /** Danh sach cau hoi goi y nhanh (quick replies) */
    private List<String> suggestions;
}
