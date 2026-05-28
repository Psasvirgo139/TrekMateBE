package com.trekmate.backend.model.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;
// reviewId is Long (BIGSERIAL FK to reviews); userId remains UUID (FK to users)

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ReviewHelpfulId implements Serializable {

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "user_id")
    private UUID userId;
}
