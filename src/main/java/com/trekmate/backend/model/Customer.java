package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.FitnessLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customers",
       indexes = @Index(name = "idx_customers_user_id", columnList = "user_id"))
@EntityListeners(AuditingEntityListener.class)
public class Customer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationality", length = 80)
    private String nationality;

    @Column(name = "home_address", columnDefinition = "TEXT")
    private String homeAddress;

    // JSONB: {"name":"...","phone":"...","relation":"..."}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emergency_contact", columnDefinition = "jsonb")
    @Builder.Default
    private java.util.Map<String, Object> emergencyContact = new java.util.HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level", length = 30)
    private FitnessLevel fitnessLevel;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(name = "preferred_language", length = 5, nullable = false)
    @Builder.Default
    private String preferredLanguage = "vi";

    @Column(name = "total_tours_joined", nullable = false)
    @Builder.Default
    private Integer totalToursJoined = 0;
}

