package com.travolish.traveller.hotel.service;

import java.util.List;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.PropertySeoMeta;

public interface PropertySeoService {

    /** §1.1 — Generate page title: Name + Type + Location + Key Feature + Brand */
    String generatePageTitle(Hotel hotel);

    /** §1.2 — AI meta description, 120–160 chars */
    String generateMetaDescription(Hotel hotel);

    /** §1.3 — URL slug: /country/state/city/property-name/listing-id */
    String generateUrlSlug(Hotel hotel);

    /** §2 — AI-generated image title based on context */
    String generateImageTitle(String imageContext, Hotel hotel);

    /** §2 — SEO-friendly file name for an image */
    String generateSeoFileName(String originalFileName, Hotel hotel);

    /** §3 — JSON-LD schema markup (Hotel + LocalBusiness) */
    String generateSchemaMarkup(Hotel hotel);

    /** §20 — AI SEO description with keywords, location, USP, target audience */
    String generateSeoDescription(Hotel hotel);

    /** §20 — Guest match tags (Family, Business, Couples, Solo, Senior, Young) */
    List<String> generateGuestMatchTags(Hotel hotel);

    /** §20 — AI Value Score (0.0–10.0) */
    Double computeValueScore(Hotel hotel);

    /** §20 — AI Location Score (0.0–10.0) */
    Double computeLocationScore(Hotel hotel);

    /** §20 — AI Translation */
    String translateText(String text, String targetLanguage);

    /** Generate and persist full SEO meta for a hotel (called on DRAFT→LIVE) */
    PropertySeoMeta generateAndSaveSeoMeta(Hotel hotel);

    /** Retrieve existing SEO meta, or generate if absent */
    PropertySeoMeta getOrGenerateSeoMeta(Long hotelId);
}
