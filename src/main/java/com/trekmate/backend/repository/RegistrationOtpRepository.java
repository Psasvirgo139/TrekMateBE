package com.trekmate.backend.repository;

import com.trekmate.backend.model.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, String> {}
