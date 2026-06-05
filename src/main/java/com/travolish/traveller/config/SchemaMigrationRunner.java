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
            // Column may already be nullable — not an error
            log.debug("Migration skipped (already applied or not applicable): {}.{} — {}", table, column, e.getMessage());
        }
    }
}
