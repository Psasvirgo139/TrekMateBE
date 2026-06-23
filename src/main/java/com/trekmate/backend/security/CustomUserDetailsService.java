package com.trekmate.backend.security;

import com.trekmate.backend.model.User;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (guideRepository.existsByUserId(user.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_GUIDE"));
        }
        if (customerRepository.existsByUserId(user.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }

        return new AuthUserDetails(user, authorities);
    }
}
