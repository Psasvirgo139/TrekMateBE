package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.CertificationDto;
import com.trekmate.backend.dto.request.CustomerProfileUpdateRequest;
import com.trekmate.backend.dto.request.GuideProfileUpdateRequest;
import com.trekmate.backend.dto.response.CustomerProfileResponse;
import com.trekmate.backend.dto.response.GuideProfileResponse;

import java.util.UUID;

public interface UserProfileService {
    CustomerProfileResponse getCustomerProfile(UUID userId);
    CustomerProfileResponse updateCustomerProfile(UUID userId, CustomerProfileUpdateRequest request);
    GuideProfileResponse getGuideProfile(UUID userId);
    GuideProfileResponse updateGuideProfile(UUID userId, GuideProfileUpdateRequest request);
    GuideProfileResponse addGuideCertification(UUID userId, CertificationDto certDto);
    GuideProfileResponse removeGuideCertification(UUID userId, String certName);
}
