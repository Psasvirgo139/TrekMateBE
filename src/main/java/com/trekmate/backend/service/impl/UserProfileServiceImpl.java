package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.CertificationDto;
import com.trekmate.backend.dto.request.CustomerProfileUpdateRequest;
import com.trekmate.backend.dto.request.GuideProfileUpdateRequest;
import com.trekmate.backend.dto.response.CustomerProfileResponse;
import com.trekmate.backend.dto.response.GuideProfileResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.DepartureGuideRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.repository.ReviewRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.dto.response.GuideTourHistoryResponse;
import com.trekmate.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;
    private final BookingRepository bookingRepository;
    private final DepartureGuideRepository departureGuideRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public CustomerProfileResponse getCustomerProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Customer customer = customerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setUser(user);
                    String defaultName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Customer";
                    newCustomer.setFullName(defaultName);
                    newCustomer.setEmergencyContact(new java.util.HashMap<>());
                    newCustomer.setPreferredLanguage("vi");
                    return customerRepository.save(newCustomer);
                });

        long totalTours = bookingRepository.countByUserIdAndStatus(userId, BookingStatus.COMPLETED);

        return new CustomerProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                customer.getFullName(),
                customer.getAvatarUrl(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNationality(),
                customer.getHomeAddress(),
                customer.getEmergencyContact(),
                customer.getFitnessLevel(),
                customer.getMedicalNotes(),
                customer.getPreferredLanguage(),
                totalTours
        );
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateCustomerProfile(UUID userId, CustomerProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Customer profile not found"));

        // Validate and update phone
        if (StringUtils.hasText(request.phone()) && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.phone());
        } else if (!StringUtils.hasText(request.phone())) {
            user.setPhone(null);
        }

        // Update Customer fields
        customer.setFullName(request.fullName());
        customer.setAvatarUrl(request.avatarUrl());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());
        customer.setNationality(request.nationality());
        customer.setHomeAddress(request.homeAddress());
        customer.setEmergencyContact(request.emergencyContact() != null ? request.emergencyContact() : new HashMap<>());
        customer.setFitnessLevel(request.fitnessLevel());
        customer.setMedicalNotes(request.medicalNotes());
        customer.setPreferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : "vi");

        // Synchronize name and avatar to Guide if present
        guideRepository.findByUserId(userId).ifPresent(guide -> {
            guide.setDisplayName(request.fullName());
            if (StringUtils.hasText(request.avatarUrl())) {
                guide.setAvatarUrl(request.avatarUrl());
            }
        });

        customerRepository.save(customer);

        long totalTours = bookingRepository.countByUserIdAndStatus(userId, BookingStatus.COMPLETED);

        return new CustomerProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                customer.getFullName(),
                customer.getAvatarUrl(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNationality(),
                customer.getHomeAddress(),
                customer.getEmergencyContact(),
                customer.getFitnessLevel(),
                customer.getMedicalNotes(),
                customer.getPreferredLanguage(),
                totalTours
        );
    }

    @Override
    @Transactional
    public GuideProfileResponse getGuideProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Guide guide = guideRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Guide newGuide = new Guide();
                    newGuide.setUser(user);
                    String defaultName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Guide";
                    newGuide.setDisplayName(defaultName);
                    newGuide.setLanguages(new java.util.ArrayList<>());
                    newGuide.setSpecializations(new java.util.ArrayList<>());
                    newGuide.setCertifications(new java.util.ArrayList<>());
                    newGuide.setIsAvailable(true);
                    return guideRepository.save(newGuide);
                });

        long totalToursLed = departureGuideRepository.countByGuideIdAndDepartureStatus(userId, DepartureStatus.COMPLETED);

        return new GuideProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                guide.getDisplayName(),
                guide.getAvatarUrl(),
                guide.getBio(),
                guide.getHomeProvince(),
                guide.getExperienceYears(),
                guide.getLanguages(),
                guide.getSpecializations(),
                guide.getIdCardNumber(),
                guide.getIdCardVerified(),
                guide.getAvgRating(),
                guide.getTotalReviews(),
                totalToursLed,
                guide.getIsAvailable(),
                mapCertifications(guide.getCertifications()),
                getToursLedHistory(userId)
        );
    }

    @Override
    @Transactional
    public GuideProfileResponse updateGuideProfile(UUID userId, GuideProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Guide guide = guideRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_GUIDE_NOT_FOUND));

        // Validate and update phone
        if (StringUtils.hasText(request.phone()) && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.phone());
        } else if (!StringUtils.hasText(request.phone())) {
            user.setPhone(null);
        }

        // Update Guide fields
        guide.setDisplayName(request.displayName());
        guide.setAvatarUrl(request.avatarUrl());
        guide.setBio(request.bio());
        guide.setHomeProvince(request.homeProvince());
        guide.setExperienceYears(request.experienceYears() != null ? request.experienceYears() : 0);
        guide.setLanguages(request.languages() != null ? request.languages() : new ArrayList<>());
        guide.setSpecializations(request.specializations() != null ? request.specializations() : new ArrayList<>());
        guide.setIdCardNumber(request.idCardNumber());
        if (request.isAvailable() != null) {
            guide.setIsAvailable(request.isAvailable());
        }

        // Synchronize name and avatar to Customer if present
        customerRepository.findByUserId(userId).ifPresent(customer -> {
            customer.setFullName(request.displayName());
            if (StringUtils.hasText(request.avatarUrl())) {
                customer.setAvatarUrl(request.avatarUrl());
            }
        });

        guideRepository.save(guide);

        long totalToursLed = departureGuideRepository.countByGuideIdAndDepartureStatus(userId, DepartureStatus.COMPLETED);

        return new GuideProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                guide.getDisplayName(),
                guide.getAvatarUrl(),
                guide.getBio(),
                guide.getHomeProvince(),
                guide.getExperienceYears(),
                guide.getLanguages(),
                guide.getSpecializations(),
                guide.getIdCardNumber(),
                guide.getIdCardVerified(),
                guide.getAvgRating(),
                guide.getTotalReviews(),
                totalToursLed,
                guide.getIsAvailable(),
                mapCertifications(guide.getCertifications()),
                getToursLedHistory(userId)
        );
    }

    @Override
    @Transactional
    public GuideProfileResponse addGuideCertification(UUID userId, CertificationDto certDto) {
        Guide guide = guideRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_GUIDE_NOT_FOUND));

        List<Map<String, Object>> certifications = guide.getCertifications();
        if (certifications == null) {
            certifications = new ArrayList<>();
        }

        // Check for duplicates
        boolean exists = certifications.stream()
                .anyMatch(m -> certDto.name().equalsIgnoreCase((String) m.get("name")));
        if (exists) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Certification with this name already exists");
        }

        // Create DB map
        Map<String, Object> certMap = new HashMap<>();
        certMap.put("name", certDto.name());
        certMap.put("issued_by", certDto.issuedBy());
        certMap.put("year", certDto.year());

        certifications.add(certMap);
        guide.setCertifications(certifications);
        guideRepository.save(guide);

        return getGuideProfile(userId);
    }

    @Override
    @Transactional
    public GuideProfileResponse removeGuideCertification(UUID userId, String certName) {
        Guide guide = guideRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_GUIDE_NOT_FOUND));

        List<Map<String, Object>> certifications = guide.getCertifications();
        if (certifications == null || certifications.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No certifications found for this guide");
        }

        // Find and remove
        boolean removed = certifications.removeIf(m -> certName.equalsIgnoreCase((String) m.get("name")));
        if (!removed) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Certification not found with name: " + certName);
        }

        guide.setCertifications(certifications);
        guideRepository.save(guide);

        return getGuideProfile(userId);
    }

    private List<CertificationDto> mapCertifications(List<Map<String, Object>> certsList) {
        if (certsList == null) return new ArrayList<>();
        return certsList.stream()
                .map(m -> new CertificationDto(
                        (String) m.get("name"),
                        (String) m.get("issued_by"),
                        m.get("year") instanceof Number ? ((Number) m.get("year")).intValue() : null
                ))
                .toList();
    }

    private List<GuideTourHistoryResponse> getToursLedHistory(UUID userId) {
        return departureGuideRepository.findByGuideId(userId).stream()
                .filter(dg -> dg.getDeparture().getStatus() != DepartureStatus.CANCELLED)
                .map(dg -> {
                    var departure = dg.getDeparture();
                    Double avgRating = reviewRepository.avgRatingByDeparture(departure.getId());
                    return new GuideTourHistoryResponse(
                            departure.getId(),
                            departure.getTour().getTitle(),
                            departure.getDepartureDate(),
                            avgRating,
                            departure.getStatus().name()
                    );
                })
                .sorted((a, b) -> b.departureDate().compareTo(a.departureDate()))
                .toList();
    }
}
