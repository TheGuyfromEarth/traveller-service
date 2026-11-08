package com.travolish.traveller.listing.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.model.PropertyCategory;
import com.travolish.traveller.hotel.model.PropertySubType;
import com.travolish.traveller.hotel.model.StayType;
import com.travolish.traveller.hotel.model.TargetGuest;
import com.travolish.traveller.listing.dto.CategoryDTO;
import com.travolish.traveller.listing.dto.SubTypeDTO;

/**
 * Step 1 & Step 2 support — purely static, no DB required.
 * Returns the full taxonomy so the frontend can drive the wizard.
 */
@RestController
@RequestMapping("/api/listing")
public class TaxonomyController {

    private static final Map<PropertyCategory, CategoryDTO> CATEGORY_META = Map.of(
        PropertyCategory.HOTEL,              new CategoryDTO("HOTEL", "Hotel", "Professional hospitality property"),
        PropertyCategory.APARTMENT,          new CategoryDTO("APARTMENT", "Apartment", "Self-catering apartment or flat"),
        PropertyCategory.HOLIDAY_RENTAL,     new CategoryDTO("HOLIDAY_RENTAL", "House & Villa", "Entire home or villa"),
        PropertyCategory.RESORT,             new CategoryDTO("RESORT", "Resort", "Resort with extensive facilities"),
        PropertyCategory.HOSTEL,             new CategoryDTO("HOSTEL", "Hostel", "Budget shared accommodation"),
        PropertyCategory.GUEST_HOUSE_AND_BB, new CategoryDTO("GUEST_HOUSE_AND_BB", "Guest House & B&B", "Intimate hosted accommodation"),
        PropertyCategory.NATURE_AND_OUTDOOR, new CategoryDTO("NATURE_AND_OUTDOOR", "Nature & Outdoor Stay", "Eco, farm, or outdoor experience"),
        PropertyCategory.UNIQUE_STAY,        new CategoryDTO("UNIQUE_STAY", "Unique Stay", "One-of-a-kind accommodation")
    );

    /** Step 1 — all 8 categories in display order. */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getCategories() {
        List<PropertyCategory> order = List.of(
            PropertyCategory.HOTEL, PropertyCategory.APARTMENT,
            PropertyCategory.HOLIDAY_RENTAL, PropertyCategory.RESORT,
            PropertyCategory.HOSTEL, PropertyCategory.GUEST_HOUSE_AND_BB,
            PropertyCategory.NATURE_AND_OUTDOOR, PropertyCategory.UNIQUE_STAY
        );
        return ResponseEntity.ok(order.stream().map(CATEGORY_META::get).toList());
    }

    /** Step 2 — sub-types filtered by chosen category. */
    @GetMapping("/categories/{category}/sub-types")
    public ResponseEntity<List<SubTypeDTO>> getSubTypes(@PathVariable String category) {
        PropertyCategory cat;
        try {
            cat = PropertyCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        List<SubTypeDTO> result = Arrays.stream(PropertySubType.values())
            .filter(st -> st.category == cat)
            .map(st -> new SubTypeDTO(st.name(), formatLabel(st.name()), cat.name()))
            .toList();
        return ResponseEntity.ok(result);
    }

    /** All sub-types grouped by category — useful for frontend to pre-cache. */
    @GetMapping("/sub-types")
    public ResponseEntity<Map<String, List<SubTypeDTO>>> getAllSubTypes() {
        Map<String, List<SubTypeDTO>> grouped = Arrays.stream(PropertySubType.values())
            .collect(Collectors.groupingBy(
                st -> st.category.name(),
                Collectors.mapping(
                    st -> new SubTypeDTO(st.name(), formatLabel(st.name()), st.category.name()),
                    Collectors.toList()
                )
            ));
        return ResponseEntity.ok(grouped);
    }

    /** Step 4 support — all target guest options. */
    @GetMapping("/target-guests")
    public ResponseEntity<List<Map<String, String>>> getTargetGuests() {
        return ResponseEntity.ok(
            Arrays.stream(TargetGuest.values())
                .map(tg -> Map.of("key", tg.name(), "label", formatLabel(tg.name())))
                .toList()
        );
    }

    /** Step 4 support — all stay type options. */
    @GetMapping("/stay-types")
    public ResponseEntity<List<Map<String, String>>> getStayTypes() {
        return ResponseEntity.ok(
            Arrays.stream(StayType.values())
                .map(st -> Map.of("key", st.name(), "label", formatLabel(st.name())))
                .toList()
        );
    }

    private String formatLabel(String name) {
        return Arrays.stream(name.split("_"))
            .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
}
