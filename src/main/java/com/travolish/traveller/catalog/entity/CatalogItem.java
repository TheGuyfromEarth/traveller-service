package com.travolish.traveller.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_items", indexes = {
    @Index(name = "idx_catalog_type", columnList = "item_type"),
    @Index(name = "idx_catalog_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // CATEGORY, AMENITY

    @Column(name = "item_group", length = 100)
    private String itemGroup; // "Property type", "Transport", "Food", "Business", etc.

    @Column(length = 100)
    private String icon; // icon name / key used in UI

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, DISABLED

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
