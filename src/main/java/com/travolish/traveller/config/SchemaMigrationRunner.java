package com.travolish.traveller.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies manual schema changes that Hibernate ddl-auto:update cannot handle
 * (e.g. dropping NOT NULL constraints that were removed from entities).
 * Each statement is safe to run multiple times.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        // PricingRule.roomId: allow null so hotel-wide rules (no specific room) can be created.
        dropNotNullIfExists("pricing_rules", "room_id");

        // Hotel.status: Hibernate adds the column as nullable first, then we backfill
        // existing rows with LIVE and add the NOT NULL constraint.
        backfillHotelStatus();

        // Seed amenities for hotels that have none
        seedHotelAmenities();

        // reviews.status: add ESCALATED to the check constraint if missing
        fixReviewsStatusConstraint();

        // rooms.capacity: backfill null values with default 2
        backfillRoomCapacity();

        // users: backfill createdAt and role for existing rows
        backfillUserCreatedAt();
        backfillUserRoles();

        // Property Listing Module — Phase 11 migrations
        migrateHotelNewColumns();
        migrateRoomNewColumns();
        migrateReviewSubRatings();
        createPropertyPoliciesTable();
        createNearbyAttractionsTable();
        createPropertyContactsTable();
        createPropertySeoMetaTable();
        createBookingPaymentConfigTable();
        migratePricingRuleNewColumns();

        // Listing Wizard — Phase F migrations
        migrateHotelTaxonomyColumns();
        createListingDraftTables();
    }

    private void backfillHotelStatus() {
        try {
            // Set LIVE as default for any existing rows that have null status
            int updated = jdbcTemplate.update(
                "UPDATE hotels SET status = 'LIVE' WHERE status IS NULL"
            );
            if (updated > 0) {
                log.info("Migration: backfilled {} hotel(s) with default status LIVE", updated);
            }
        } catch (Exception e) {
            log.debug("Hotel status backfill skipped: {}", e.getMessage());
        }
    }

    private void seedHotelAmenities() {
        try {
            // Only seed if the table exists and has no rows yet
            Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hotel_amenities", Integer.class);
            if (existingCount != null && existingCount > 0) return;

            // Seed common amenities for all hotels
            String[] amenities = { "WiFi", "Air conditioning", "Free parking", "24-hour front desk", "Room service" };
            // Get all hotel IDs
            java.util.List<Long> hotelIds = jdbcTemplate.queryForList("SELECT id FROM hotels", Long.class);
            for (Long hotelId : hotelIds) {
                for (String amenity : amenities) {
                    jdbcTemplate.update(
                        "INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (?, ?)", hotelId, amenity);
                }
            }
            log.info("Migration: seeded amenities for {} hotels", hotelIds.size());
        } catch (Exception e) {
            log.debug("Hotel amenities seed skipped: {}", e.getMessage());
        }
    }

    private void backfillUserCreatedAt() {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE users SET created_at = NOW() WHERE created_at IS NULL"
            );
            if (updated > 0) log.info("Migration: backfilled created_at for {} user(s)", updated);
        } catch (Exception e) {
            log.debug("User createdAt backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillUserRoles() {
        try {
            // Users who have hotels → HOST
            int hosts = jdbcTemplate.update(
                "UPDATE users SET role = 'HOST' WHERE role IS NULL " +
                "AND id IN (SELECT DISTINCT host_id FROM hotels WHERE host_id IS NOT NULL)"
            );
            // Everyone else → GUEST
            int guests = jdbcTemplate.update(
                "UPDATE users SET role = 'GUEST' WHERE role IS NULL"
            );
            if (hosts + guests > 0)
                log.info("Migration: assigned roles — {} HOST, {} GUEST", hosts, guests);
        } catch (Exception e) {
            log.debug("User role backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillRoomCapacity() {
        try {
            int updated = jdbcTemplate.update("UPDATE rooms SET capacity = 2 WHERE capacity IS NULL");
            if (updated > 0) log.info("Migration: backfilled {} room(s) with default capacity 2", updated);
        } catch (Exception e) {
            log.debug("Room capacity backfill skipped: {}", e.getMessage());
        }
    }

    private void fixReviewsStatusConstraint() {
        try {
            // Drop the old constraint (which lacked ESCALATED) and replace it
            jdbcTemplate.execute("ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_status_check");
            jdbcTemplate.execute(
                "ALTER TABLE reviews ADD CONSTRAINT reviews_status_check " +
                "CHECK (status IN ('PENDING','APPROVED','REJECTED','FLAGGED','ESCALATED'))"
            );
            log.info("Migration: reviews_status_check constraint updated to include ESCALATED");
        } catch (Exception e) {
            log.debug("reviews_status_check migration skipped: {}", e.getMessage());
        }
    }

    private void dropNotNullIfExists(String table, String column) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " DROP NOT NULL"
            );
            log.info("Migration applied: {}.{} is now nullable", table, column);
        } catch (Exception e) {
            log.debug("Migration skipped (already applied or not applicable): {}.{} — {}", table, column, e.getMessage());
        }
    }

    // ── Property Listing Module migrations ───────────────────────────────────

    private void migrateHotelNewColumns() {
        String[][] columns = {
            {"state", "VARCHAR(255)"},
            {"postal_code", "VARCHAR(50)"},
            {"property_type", "VARCHAR(50)"},
            {"star_rating", "INTEGER"},
            {"brand", "VARCHAR(255)"},
            {"year_built", "INTEGER"},
            {"last_renovated", "INTEGER"},
            {"distance_to_airport", "VARCHAR(100)"},
            {"distance_to_train", "VARCHAR(100)"},
            {"distance_to_city_centre", "VARCHAR(100)"},
            {"distance_to_beach", "VARCHAR(100)"},
            {"total_rooms", "INTEGER"},
            {"total_floors", "INTEGER"},
            {"total_buildings", "INTEGER"},
            {"property_size", "VARCHAR(100)"},
            {"reception_hours", "VARCHAR(100)"},
            {"twenty_four_hour_front_desk", "BOOLEAN DEFAULT FALSE"},
            {"maximum_stay", "INTEGER"},
            {"booking_window", "INTEGER"},
            {"last_minute_booking", "BOOLEAN DEFAULT FALSE"},
            {"same_day_booking", "BOOLEAN DEFAULT FALSE"},
            {"cover_photo_title", "VARCHAR(300)"},
            {"three_sixty_tour_url", "VARCHAR(1000)"},
        };
        for (String[] col : columns) addColumnIfMissing("hotels", col[0], col[1]);

        // Collection tables for new Hotel @ElementCollection fields
        createCollectionTableIfMissing("hotel_languages", "hotel_id BIGINT", "language VARCHAR(100)");
        createCollectionTableIfMissing("hotel_drone_photos", "hotel_id BIGINT", "photo_url VARCHAR(1000)");
        createCollectionTableIfMissing("hotel_meal_options", "hotel_id BIGINT", "meal_option VARCHAR(50)");
        createCollectionTableIfMissing("hotel_transportation", "hotel_id BIGINT", "option VARCHAR(255)");
        createCollectionTableIfMissing("hotel_guest_services", "hotel_id BIGINT", "service VARCHAR(255)");
        createCollectionTableIfMissing("hotel_sustainability", "hotel_id BIGINT", "feature VARCHAR(255)");
        log.info("Migration: hotel new columns applied");
    }

    private void migrateRoomNewColumns() {
        String[][] columns = {
            {"name", "VARCHAR(255)"},
            {"description", "VARCHAR(2000)"},
            {"size", "DOUBLE PRECISION"},
            {"bed_type", "VARCHAR(50)"},
            {"number_of_beds", "INTEGER DEFAULT 1"},
            {"view", "VARCHAR(255)"},
            {"smoking_allowed", "BOOLEAN DEFAULT FALSE"},
            {"accessible_room", "BOOLEAN DEFAULT FALSE"},
            {"private_bathroom", "BOOLEAN DEFAULT TRUE"},
            {"weekend_price", "DOUBLE PRECISION"},
            {"seasonal_price", "DOUBLE PRECISION"},
            {"holiday_price", "DOUBLE PRECISION"},
            {"weekly_discount", "DOUBLE PRECISION"},
            {"monthly_discount", "DOUBLE PRECISION"},
            {"taxes", "DOUBLE PRECISION"},
            {"service_charges", "DOUBLE PRECISION"},
            {"security_deposit", "DOUBLE PRECISION"},
            {"currency", "VARCHAR(10) DEFAULT 'USD'"},
        };
        for (String[] col : columns) addColumnIfMissing("rooms", col[0], col[1]);
        createCollectionTableIfMissing("room_photos", "room_id BIGINT", "photo_url VARCHAR(1000)");
        createCollectionTableIfMissing("room_amenities", "room_id BIGINT", "amenity VARCHAR(255)");
        log.info("Migration: room new columns applied");
    }

    private void migrateReviewSubRatings() {
        String[][] columns = {
            {"cleanliness_rating", "INTEGER"},
            {"location_rating", "INTEGER"},
            {"value_rating", "INTEGER"},
            {"staff_rating", "INTEGER"},
            {"comfort_rating", "INTEGER"},
            {"host_response", "VARCHAR(2000)"},
            {"host_response_at", "TIMESTAMP WITH TIME ZONE"},
            {"imported_from", "VARCHAR(100)"},
        };
        for (String[] col : columns) addColumnIfMissing("reviews", col[0], col[1]);
        log.info("Migration: review sub-rating columns applied");
    }

    private void createPropertyPoliciesTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS property_policies (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  hotel_id BIGINT NOT NULL UNIQUE," +
                "  cancellation_policy VARCHAR(2000)," +
                "  refund_policy VARCHAR(2000)," +
                "  child_policy VARCHAR(1000)," +
                "  pet_policy VARCHAR(1000)," +
                "  smoking_policy VARCHAR(1000)," +
                "  visitor_policy VARCHAR(1000)," +
                "  damage_policy VARCHAR(1000)," +
                "  quiet_hours VARCHAR(255)," +
                "  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()" +
                ")"
            );
            log.info("Migration: property_policies table ready");
        } catch (Exception e) {
            log.debug("property_policies table migration skipped: {}", e.getMessage());
        }
    }

    private void createNearbyAttractionsTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS nearby_attractions (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  hotel_id BIGINT NOT NULL," +
                "  name VARCHAR(255) NOT NULL," +
                "  distance_text VARCHAR(100)," +
                "  attraction_type VARCHAR(50)" +
                ")"
            );
            log.info("Migration: nearby_attractions table ready");
        } catch (Exception e) {
            log.debug("nearby_attractions table migration skipped: {}", e.getMessage());
        }
    }

    private void createPropertyContactsTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS property_contacts (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  hotel_id BIGINT NOT NULL UNIQUE," +
                "  contact_person VARCHAR(255)," +
                "  phone VARCHAR(50)," +
                "  email VARCHAR(255)," +
                "  website VARCHAR(500)," +
                "  emergency_contact VARCHAR(255)," +
                "  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()" +
                ")"
            );
            log.info("Migration: property_contacts table ready");
        } catch (Exception e) {
            log.debug("property_contacts table migration skipped: {}", e.getMessage());
        }
    }

    private void createPropertySeoMetaTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS property_seo_meta (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  hotel_id BIGINT NOT NULL UNIQUE," +
                "  page_title VARCHAR(300)," +
                "  meta_description VARCHAR(500)," +
                "  url_slug VARCHAR(500) UNIQUE," +
                "  schema_json TEXT," +
                "  cover_image_title VARCHAR(300)," +
                "  last_generated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()" +
                ")"
            );
            log.info("Migration: property_seo_meta table ready");
        } catch (Exception e) {
            log.debug("property_seo_meta table migration skipped: {}", e.getMessage());
        }
    }

    private void createBookingPaymentConfigTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS booking_payment_configs (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  hotel_id BIGINT NOT NULL UNIQUE," +
                "  pay_full_at_booking BOOLEAN DEFAULT TRUE," +
                "  pay_at_property BOOLEAN DEFAULT FALSE," +
                "  secure_with_partial_payment BOOLEAN DEFAULT FALSE," +
                "  advance_payment_percent INTEGER DEFAULT 0," +
                "  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()" +
                ")"
            );
            createCollectionTableIfMissing("booking_payment_methods", "config_id BIGINT", "payment_method VARCHAR(100)");
            log.info("Migration: booking_payment_configs table ready");
        } catch (Exception e) {
            log.debug("booking_payment_configs table migration skipped: {}", e.getMessage());
        }
    }

    private void migratePricingRuleNewColumns() {
        addColumnIfMissing("pricing_rules", "promo_label", "VARCHAR(255)");
        addColumnIfMissing("pricing_rules", "non_monetary", "BOOLEAN DEFAULT FALSE");
        log.info("Migration: pricing_rules new columns applied");
    }

    // ── Listing Wizard migrations ─────────────────────────────────────────────

    private void migrateHotelTaxonomyColumns() {
        // Category taxonomy columns added by the listing wizard feature
        String[][] columns = {
            {"category",     "VARCHAR(50)"},
            {"stay_type",    "VARCHAR(50)"},
            {"num_bedrooms", "INTEGER"},
            {"num_bathrooms","INTEGER"},
            {"max_guests",   "INTEGER"},
            {"num_units",    "INTEGER"},
            {"check_in_time", "VARCHAR(50)"},
            {"check_out_time","VARCHAR(50)"},
        };
        for (String[] col : columns) addColumnIfMissing("hotels", col[0], col[1]);

        createCollectionTableIfMissing("hotel_sub_types",    "hotel_id BIGINT", "sub_type VARCHAR(100)");
        createCollectionTableIfMissing("hotel_target_guests","hotel_id BIGINT", "target_guest VARCHAR(100)");
        log.info("Migration: hotel taxonomy columns and collection tables ready");
    }

    private void createListingDraftTables() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS listing_drafts (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  host_id BIGINT NOT NULL," +
                "  category VARCHAR(50)," +
                "  name VARCHAR(255)," +
                "  star_rating INTEGER," +
                "  num_bedrooms INTEGER," +
                "  num_bathrooms INTEGER," +
                "  max_guests INTEGER," +
                "  num_units INTEGER," +
                "  check_in_time VARCHAR(50)," +
                "  check_out_time VARCHAR(50)," +
                "  stay_type VARCHAR(50)," +
                "  status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS'," +
                "  current_step INTEGER NOT NULL DEFAULT 1," +
                "  published_hotel_id BIGINT," +
                "  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()," +
                "  expires_at TIMESTAMP WITH TIME ZONE" +
                ")"
            );
            log.info("Migration: listing_drafts table ready");
        } catch (Exception e) {
            log.debug("listing_drafts table migration skipped: {}", e.getMessage());
        }

        createCollectionTableIfMissing("listing_draft_sub_types",
            "draft_id BIGINT", "sub_type VARCHAR(100)");
        createCollectionTableIfMissing("listing_draft_amenities",
            "draft_id BIGINT", "amenity VARCHAR(255)");
        createCollectionTableIfMissing("listing_draft_target_guests",
            "draft_id BIGINT", "target_guest VARCHAR(100)");

        log.info("Migration: listing draft collection tables ready");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + type
            );
        } catch (Exception e) {
            log.debug("addColumnIfMissing skipped {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void createCollectionTableIfMissing(String table, String fkCol, String valueCol) {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS " + table + " (" + fkCol + ", " + valueCol + ")"
            );
        } catch (Exception e) {
            log.debug("createCollectionTableIfMissing skipped {}: {}", table, e.getMessage());
        }
    }
}
