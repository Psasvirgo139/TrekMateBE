package com.trekmate.backend.utils;

import com.trekmate.backend.dto.response.AuthUserResponse;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.User;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUserMapper {

    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;

    public AuthUserResponse toResponse(User user) {
        List<String> roles = resolveRoles(user);
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                resolveDisplayName(user),
                roles,
                Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "SUSPENDED"
        );
    }

    public List<String> resolveRoles(User user) {
        List<String> roles = new ArrayList<>();
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            roles.add("ADMIN");
        }
        if (guideRepository.existsByUserId(user.getId())) {
            roles.add("GUIDE");
        }
        if (customerRepository.existsByUserId(user.getId())) {
            roles.add("CUSTOMER");
        }
        return roles;
    }

    private String resolveDisplayName(User user) {
        Optional<Guide> guide = guideRepository.findByUserId(user.getId());
        if (guide.isPresent() && guide.get().getDisplayName() != null) {
            return guide.get().getDisplayName();
        }
        Optional<Customer> customer = customerRepository.findByUserId(user.getId());
        if (customer.isPresent() && customer.get().getFullName() != null) {
            return customer.get().getFullName();
        }
        return user.getEmail();
    }
}
