package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.TourAttributeType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tour_attributes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tour_attributes_content_type", columnNames = {"content", "type"})
}, indexes = {
        @Index(name = "idx_tour_attributes_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
public class TourAttribute extends LongBaseEntity {

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TourAttributeType type;
}
