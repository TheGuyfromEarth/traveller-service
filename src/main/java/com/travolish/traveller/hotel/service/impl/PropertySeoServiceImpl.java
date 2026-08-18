package com.travolish.traveller.hotel.service.impl;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.PropertySeoMeta;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.repository.PropertySeoMetaRepository;
import com.travolish.traveller.hotel.service.PropertySeoService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PropertySeoServiceImpl implements PropertySeoService {

    private final PropertySeoMetaRepository seoMetaRepository;
    private final HotelRepository hotelRepository;

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

    // ── §1.1 ────────────────────────────────────────────────────────────────
    @Override
    public String generatePageTitle(Hotel hotel) {
        StringBuilder title = new StringBuilder(hotel.getName());
        if (hotel.getCity() != null) title.append(" ").append(hotel.getCity());
        if (hotel.getStarRating() != null) title.append(" | ").append(hotel.getStarRating()).append("★");
        if (hotel.getCategory() != null) title.append(" ").append(formatPropertyType(hotel.getCategory().name()));
        if (hotel.getBrand() != null && !hotel.getBrand().isBlank()) title.append(" – ").append(hotel.getBrand());
        String built = title.toString();
        return built.length() > 70 ? built.substring(0, 67) + "..." : built;
    }

    // ── §1.2 ────────────────────────────────────────────────────────────────
    @Override
    public String generateMetaDescription(Hotel hotel) {
        String prompt = String.format(
            "Write a hotel meta description for SEO. It must be exactly 120–160 characters long. " +
            "Include: property name, location, 2–3 top amenities, and a CTA like 'Book now'. " +
            "Property: %s, Location: %s %s, Amenities: %s. " +
            "Return only the description text, nothing else.",
            hotel.getName(),
            hotel.getCity() != null ? hotel.getCity() : "",
            hotel.getCountry() != null ? hotel.getCountry() : "",
            hotel.getAmenities() != null ? String.join(", ", hotel.getAmenities().stream().limit(4).toList()) : "WiFi, Parking"
        );
        String result = callGemini(prompt);
        if (result != null) {
            result = result.replaceAll("\"", "").trim();
            return result.length() > 160 ? result.substring(0, 157) + "..." : result;
        }
        // Fallback template
        return String.format("Stay at %s in %s. Enjoy top amenities and unbeatable comfort. Book now for the best rates.",
            hotel.getName(), hotel.getCity() != null ? hotel.getCity() : hotel.getCountry());
    }

    // ── §1.3 ────────────────────────────────────────────────────────────────
    @Override
    public String generateUrlSlug(Hotel hotel) {
        String country = slugify(hotel.getCountry());
        String state   = slugify(hotel.getState());
        String city    = slugify(hotel.getCity());
        String name    = slugify(hotel.getName());
        String id      = String.format("%04d", hotel.getId());
        return String.format("/%s/%s/%s/%s/%s", country, state, city, name, id);
    }

    // ── §2 ──────────────────────────────────────────────────────────────────
    @Override
    public String generateImageTitle(String imageContext, Hotel hotel) {
        String prompt = String.format(
            "Generate a short, descriptive image alt title (max 10 words) for a hotel photo. " +
            "Context: %s. Hotel: %s, Location: %s. Return only the title, nothing else.",
            imageContext, hotel.getName(), hotel.getCity() != null ? hotel.getCity() : hotel.getCountry()
        );
        String result = callGemini(prompt);
        return result != null ? result.trim().replaceAll("\"", "") :
            imageContext + " at " + hotel.getName();
    }

    @Override
    public String generateSeoFileName(String originalFileName, Hotel hotel) {
        String ext = originalFileName != null && originalFileName.contains(".")
            ? originalFileName.substring(originalFileName.lastIndexOf("."))
            : ".jpg";
        String base = slugify(originalFileName != null
            ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
            : "photo");
        String city = slugify(hotel.getCity());
        String name = slugify(hotel.getName());
        return base + "-" + city + "-" + name + ext;
    }

    // ── §3 ──────────────────────────────────────────────────────────────────
    @Override
    public String generateSchemaMarkup(Hotel hotel) {
        String amenitiesJson = hotel.getAmenities() != null
            ? hotel.getAmenities().stream().map(a -> "\"" + a + "\"").reduce((a, b) -> a + "," + b).orElse("")
            : "";

        return String.format("""
            {
              "@context": "https://schema.org",
              "@graph": [
                {
                  "@type": "Hotel",
                  "name": "%s",
                  "description": "%s",
                  "starRating": { "@type": "Rating", "ratingValue": "%s" },
                  "priceRange": "$$",
                  "address": {
                    "@type": "PostalAddress",
                    "streetAddress": "%s",
                    "addressLocality": "%s",
                    "addressRegion": "%s",
                    "postalCode": "%s",
                    "addressCountry": "%s"
                  },
                  "geo": {
                    "@type": "GeoCoordinates",
                    "latitude": "%s",
                    "longitude": "%s"
                  },
                  "amenityFeature": [%s],
                  "checkinTime": "%s",
                  "checkoutTime": "%s",
                  "aggregateRating": {
                    "@type": "AggregateRating",
                    "ratingValue": "%s",
                    "reviewCount": "0"
                  }
                },
                {
                  "@type": "LocalBusiness",
                  "name": "%s",
                  "address": {
                    "@type": "PostalAddress",
                    "streetAddress": "%s",
                    "addressLocality": "%s",
                    "addressCountry": "%s"
                  },
                  "telephone": "%s",
                  "url": "https://travolish.com%s"
                }
              ]
            }""",
            esc(hotel.getName()), esc(hotel.getDescription()),
            hotel.getStarRating() != null ? hotel.getStarRating() : "",
            esc(hotel.getAddress()), esc(hotel.getCity()), esc(hotel.getState()),
            esc(hotel.getPostalCode()), esc(hotel.getCountry()),
            hotel.getLatitude() != null ? hotel.getLatitude() : "",
            hotel.getLongitude() != null ? hotel.getLongitude() : "",
            amenitiesJson,
            esc(hotel.getCheckInTime()), esc(hotel.getCheckOutTime()),
            hotel.getRating() != null ? hotel.getRating() : "",
            esc(hotel.getName()), esc(hotel.getAddress()), esc(hotel.getCity()), esc(hotel.getCountry()),
            esc(hotel.getPhone()),
            generateUrlSlug(hotel)
        );
    }

    // ── §20 ─────────────────────────────────────────────────────────────────
    @Override
    public String generateSeoDescription(Hotel hotel) {
        String prompt = String.format(
            "Write an SEO-optimized property description (150–200 words) for a travel booking platform. " +
            "Include: main keyword (e.g. 'Hotel in %s'), 2 nearby landmarks with distances, " +
            "the property's top USP, and the target audience. " +
            "Property: %s, Type: %s, City: %s, Country: %s, Amenities: %s. " +
            "Write naturally, no bullet points.",
            hotel.getCity(),
            hotel.getName(),
            hotel.getCategory() != null ? hotel.getCategory().name() : "Hotel",
            hotel.getCity(), hotel.getCountry(),
            hotel.getAmenities() != null ? String.join(", ", hotel.getAmenities().stream().limit(5).toList()) : "WiFi"
        );
        String result = callGemini(prompt);
        return result != null ? result.trim() :
            "Discover " + hotel.getName() + " in " + hotel.getCity() + ". An exceptional stay awaits you.";
    }

    @Override
    public List<String> generateGuestMatchTags(Hotel hotel) {
        List<String> tags = new ArrayList<>();
        if (hotel.getAmenities() == null) return tags;
        List<String> amenities = hotel.getAmenities().stream().map(String::toLowerCase).toList();
        if (amenities.stream().anyMatch(a -> a.contains("family") || a.contains("kids") || a.contains("cot")))
            tags.add("Family");
        if (amenities.stream().anyMatch(a -> a.contains("meeting") || a.contains("business") || a.contains("wifi")))
            tags.add("Business");
        if (amenities.stream().anyMatch(a -> a.contains("spa") || a.contains("pool") || a.contains("romantic")))
            tags.add("Couples");
        if (amenities.stream().anyMatch(a -> a.contains("gym") || a.contains("solo") || a.contains("hostel")))
            tags.add("Solo");
        if (hotel.getMealOptions() != null && !hotel.getMealOptions().isEmpty())
            tags.add("Senior Friendly");
        if (tags.isEmpty()) tags.add("All Travelers");
        return tags;
    }

    @Override
    public Double computeValueScore(Hotel hotel) {
        double score = 5.0;
        if (hotel.getAmenities() != null) score += Math.min(hotel.getAmenities().size() * 0.3, 3.0);
        if (hotel.getRating() != null) score = (score + hotel.getRating() * 2) / 2;
        if (hotel.getStarRating() != null) score += hotel.getStarRating() * 0.2;
        return Math.min(10.0, Math.round(score * 10.0) / 10.0);
    }

    @Override
    public Double computeLocationScore(Hotel hotel) {
        double score = 5.0;
        if (hotel.getDistanceToAirport() != null) score += 0.5;
        if (hotel.getDistanceToCityCentre() != null) score += 0.5;
        if (hotel.getDistanceToTrain() != null) score += 0.5;
        if (hotel.getDistanceToBeach() != null) score += 0.5;
        if (hotel.getLatitude() != null && hotel.getLongitude() != null) score += 1.0;
        if (hotel.getRating() != null) score = (score + hotel.getRating() * 2) / 2;
        return Math.min(10.0, Math.round(score * 10.0) / 10.0);
    }

    @Override
    public String translateText(String text, String targetLanguage) {
        String prompt = String.format(
            "Translate the following hotel listing text to %s. " +
            "Return only the translated text, no explanations.\n\n%s",
            targetLanguage, text
        );
        String result = callGemini(prompt);
        return result != null ? result.trim() : text;
    }

    // ── Orchestration ───────────────────────────────────────────────────────
    @Override
    public PropertySeoMeta generateAndSaveSeoMeta(Hotel hotel) {
        PropertySeoMeta meta = seoMetaRepository.findByHotelId(hotel.getId())
            .orElse(PropertySeoMeta.builder().hotelId(hotel.getId()).build());

        meta.setPageTitle(generatePageTitle(hotel));
        meta.setMetaDescription(generateMetaDescription(hotel));
        meta.setUrlSlug(generateUrlSlug(hotel));
        meta.setSchemaJson(generateSchemaMarkup(hotel));
        meta.setCoverImageTitle(generateImageTitle("cover photo", hotel));
        meta.setLastGeneratedAt(OffsetDateTime.now());

        log.info("SEO meta generated for hotel {}: title='{}', slug='{}'",
            hotel.getId(), meta.getPageTitle(), meta.getUrlSlug());
        return seoMetaRepository.save(meta);
    }

    @Override
    public PropertySeoMeta getOrGenerateSeoMeta(Long hotelId) {
        return seoMetaRepository.findByHotelId(hotelId).orElseGet(() -> {
            Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelId));
            return generateAndSaveSeoMeta(hotel);
        });
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) return null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );
            ResponseEntity<Map> resp = restTemplate.exchange(
                GEMINI_URL + geminiApiKey, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) resp.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
        }
        return null;
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) return "_";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "_" : normalized;
    }

    private String formatPropertyType(String type) {
        return type == null ? "" : type.replace("_", " ").toLowerCase()
            .substring(0, 1).toUpperCase() + type.replace("_", " ").toLowerCase().substring(1);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\\\"").replace("\n", " ");
    }
}
