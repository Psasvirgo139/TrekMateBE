package com.trekmate.backend.model;

import com.trekmate.backend.model.embeddable.DepartureGuideId;
import com.trekmate.backend.model.enums.GuideRoleInTour;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "departure_guides", indexes = {
        @Index(name = "idx_dep_guides_guide",     columnList = "guide_id"),
        @Index(name = "idx_dep_guides_guide_dep", columnList = "guide_id, departure_id")
})
public class DepartureGuide {

    @EmbeddedId
    private DepartureGuideId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("departureId")
    @JoinColumn(name = "departure_id")
    private TourDeparture departure;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("guideId")
    @JoinColumn(name = "guide_id")
    private Guide guide;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private GuideRoleInTour role = GuideRoleInTour.LEAD;

    @Column(name = "confirmed_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime confirmedAt;
}

