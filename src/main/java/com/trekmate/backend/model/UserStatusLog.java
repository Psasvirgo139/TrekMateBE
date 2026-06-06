package com.trekmate.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ghi lai lich su thay doi trang thai user (ban, unban, approve).
 * Khong xoa user — chi luu audit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_status_logs", indexes = {
        @Index(name = "idx_user_status_logs_user", columnList = "user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class UserStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;
}
