package com.travolish.traveller.catalog.init;

import com.travolish.traveller.catalog.entity.CatalogItem;
import com.travolish.traveller.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogDataInitializer implements CommandLineRunner {

    private final CatalogItemRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        log.info("Seeding default catalog items...");
        repository.saveAll(defaultItems());
        log.info("Catalog seed complete ({} items)", repository.count());
    }

    private List<CatalogItem> defaultItems() {
        return List.of(
            // ── Categories ────────────────────────────────────────────────────
            item("Hotel",          "CATEGORY", "Property type", "Building",    1),
            item("Villa",          "CATEGORY", "Property type", "Castle",      2),
            item("Resort",         "CATEGORY", "Property type", "Umbrella",    3),
            item("Apartment",      "CATEGORY", "Property type", "Home",        4),
            item("Hostel",         "CATEGORY", "Property type", "Bunk",        5),
            item("Guesthouse",     "CATEGORY", "Property type", "Door",        6),
            item("Boutique Hotel", "CATEGORY", "Property type", "Star",        7),

            // ── Amenities — Comfort ───────────────────────────────────────────
            item("WiFi",              "AMENITY", "Comfort",   "Wifi",       10),
            item("Air conditioning",  "AMENITY", "Comfort",   "Wind",       11),
            item("Heating",           "AMENITY", "Comfort",   "Thermometer",12),
            item("Private bathroom",  "AMENITY", "Comfort",   "Bath",       13),
            item("Balcony",           "AMENITY", "Comfort",   "Sun",        14),

            // ── Amenities — Recreation ────────────────────────────────────────
            item("Swimming pool", "AMENITY", "Recreation", "Waves",     20),
            item("Gym",           "AMENITY", "Recreation", "Dumbbell",  21),
            item("Spa",           "AMENITY", "Recreation", "Flower",    22),
            item("Beach access",  "AMENITY", "Recreation", "Umbrella",  23),

            // ── Amenities — Food ──────────────────────────────────────────────
            item("Breakfast included", "AMENITY", "Food", "Coffee",     30),
            item("Restaurant",         "AMENITY", "Food", "Utensils",   31),
            item("Room service",       "AMENITY", "Food", "Bell",       32),
            item("Kitchen",            "AMENITY", "Food", "ChefHat",    33),

            // ── Amenities — Transport ─────────────────────────────────────────
            item("Airport pickup", "AMENITY", "Transport", "Car",      40),
            item("Parking",        "AMENITY", "Transport", "Parking",  41),
            item("EV charging",    "AMENITY", "Transport", "Zap",      42),

            // ── Amenities — Business ──────────────────────────────────────────
            item("Workspace",        "AMENITY", "Business", "Laptop",  50),
            item("Meeting room",     "AMENITY", "Business", "Users",   51),
            item("Conference hall",  "AMENITY", "Business", "Monitor", 52),

            // ── Amenities — Policies ──────────────────────────────────────────
            item("Pet friendly",   "AMENITY", "Policies", "PawPrint", 60),
            item("Child friendly", "AMENITY", "Policies", "Baby",     61),
            item("Smoke-free",     "AMENITY", "Policies", "Wind",     62)
        );
    }

    private CatalogItem item(String name, String type, String group, String icon, int order) {
        return CatalogItem.builder()
                .name(name)
                .itemType(type)
                .itemGroup(group)
                .icon(icon)
                .status("ACTIVE")
                .displayOrder(order)
                .usageCount(0)
                .build();
    }
}
