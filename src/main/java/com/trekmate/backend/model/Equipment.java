package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.EquipmentCondition;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "equipment", indexes = {
        @Index(name = "idx_equipment_category", columnList = "category_id"),
        @Index(name = "idx_equipment_active",   columnList = "is_active, available_stock")
})
@EntityListeners(AuditingEntityListener.class)
public class Equipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EquipmentCategory category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    private Short totalStock = 0;

    @Column(name = "available_stock", nullable = false)
    @Builder.Default
    private Short availableStock = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    @Builder.Default
    private EquipmentCondition condition = EquipmentCondition.GOOD;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    // JSONB: {}
    @Column(name = "specifications", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String specifications = "{}";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
