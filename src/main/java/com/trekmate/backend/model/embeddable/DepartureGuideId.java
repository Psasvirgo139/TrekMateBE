package com.trekmate.backend.model.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class DepartureGuideId implements Serializable {

    @Column(name = "departure_id")
    private UUID departureId;

    @Column(name = "guide_id")
    private UUID guideId;
}
