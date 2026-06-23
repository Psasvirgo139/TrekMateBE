package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {

    @NotNull(message = "Departure ID is required")
    private UUID departureId;

    @NotNull(message = "Number of participants is required")
    @Min(value = 1, message = "Participants must be at least 1")
    private Short numParticipants;

    @NotNull(message = "Join tour option is required")
    private Boolean isJoinTour;

    private String specialRequests;

    @NotEmpty(message = "Participants details must be provided")
    private List<Map<String, Object>> participantsInfo;

    private List<EquipmentRentalRequest> rentals;
}
