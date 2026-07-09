package com.trekmate.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_locations_name", columnList = "name")
})
@EntityListeners(AuditingEntityListener.class)
public class Location extends LongBaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
