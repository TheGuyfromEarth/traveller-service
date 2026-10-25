package com.travolish.traveller.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "saved_searches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Optional human-given label, e.g. "Weekend in Goa". */
    private String name;

    private String destination;
    private String checkIn;
    private String checkOut;
    private Integer adults;
    private Integer children;

    /** JSON-encoded extra filters (price range, amenities, etc.) */
    @Column(columnDefinition = "TEXT")
    private String filtersJson;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
