package com.trekmate.backend.repository;

import com.trekmate.backend.model.UserStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserStatusLogRepository extends JpaRepository<UserStatusLog, UUID> {
}
