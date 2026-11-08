package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * §1 — Listing page SEO metadata (auto-generated on DRAFT→LIVE transition).
 * §3 — Schema markup (Hotel + LocalBusiness JSON-LD).
 */
@Entity
@Table(name = "property_seo_meta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySeoMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false, unique = true)
    private Long hotelId;

    // §1.1 — Auto-generated page title: Name + Type + Location + Key Feature + Brand
    @Column(length = 300)
    private String pageTitle;

    // §1.2 — AI-generated meta description, 120–160 chars
    @Column(length = 500)
    private String metaDescription;

    // §1.3 — URL slug: /country/state/city/property-name/listing-id
    @Column(unique = true, length = 500)
    private String urlSlug;

    // §3 — JSON-LD schema markup (Hotel + LocalBusiness schemas)
    @Column(columnDefinition = "TEXT")
    private String schemaJson;

    // §2 — AI-generated cover image title
    private String coverImageTitle;

    @Builder.Default
    private OffsetDateTime lastGeneratedAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
