package com.travolish.traveller.hotel.model;

public enum PropertySubType {

    // ── HOTEL ────────────────────────────────────────────────────────────────
    LUXURY_HOTEL(PropertyCategory.HOTEL),
    BOUTIQUE_HOTEL(PropertyCategory.HOTEL),
    BUSINESS_HOTEL(PropertyCategory.HOTEL),
    RESORT_HOTEL(PropertyCategory.HOTEL),
    AIRPORT_HOTEL(PropertyCategory.HOTEL),
    CITY_HOTEL(PropertyCategory.HOTEL),
    SPA_HOTEL(PropertyCategory.HOTEL),
    WELLNESS_HOTEL(PropertyCategory.HOTEL),
    HERITAGE_HOTEL(PropertyCategory.HOTEL),
    DESIGN_HOTEL(PropertyCategory.HOTEL),
    ECO_HOTEL(PropertyCategory.HOTEL),
    APARTHOTEL(PropertyCategory.HOTEL),
    CONFERENCE_HOTEL(PropertyCategory.HOTEL),
    PALACE_HOTEL(PropertyCategory.HOTEL),

    // ── APARTMENT ────────────────────────────────────────────────────────────
    APARTMENT(PropertyCategory.APARTMENT),
    SERVICED_APARTMENT(PropertyCategory.APARTMENT),
    STUDIO_APARTMENT(PropertyCategory.APARTMENT),
    LOFT(PropertyCategory.APARTMENT),
    DUPLEX(PropertyCategory.APARTMENT),
    TRIPLEX(PropertyCategory.APARTMENT),
    PENTHOUSE(PropertyCategory.APARTMENT),
    EXECUTIVE_APARTMENT(PropertyCategory.APARTMENT),
    CORPORATE_APARTMENT(PropertyCategory.APARTMENT),
    HOLIDAY_APARTMENT(PropertyCategory.APARTMENT),
    SHARED_APARTMENT(PropertyCategory.APARTMENT),

    // ── HOLIDAY_RENTAL ───────────────────────────────────────────────────────
    HOUSE(PropertyCategory.HOLIDAY_RENTAL),
    HOLIDAY_HOME(PropertyCategory.HOLIDAY_RENTAL),
    VILLA(PropertyCategory.HOLIDAY_RENTAL),
    LUXURY_VILLA(PropertyCategory.HOLIDAY_RENTAL),
    BEACH_HOUSE(PropertyCategory.HOLIDAY_RENTAL),
    BUNGALOW(PropertyCategory.HOLIDAY_RENTAL),
    CHALET(PropertyCategory.HOLIDAY_RENTAL),
    CABIN(PropertyCategory.HOLIDAY_RENTAL),
    COTTAGE(PropertyCategory.HOLIDAY_RENTAL),
    FARMHOUSE(PropertyCategory.HOLIDAY_RENTAL),
    COUNTRY_HOUSE(PropertyCategory.HOLIDAY_RENTAL),
    MANSION(PropertyCategory.HOLIDAY_RENTAL),

    // ── RESORT ───────────────────────────────────────────────────────────────
    BEACH_RESORT(PropertyCategory.RESORT),
    ISLAND_RESORT(PropertyCategory.RESORT),
    MOUNTAIN_RESORT(PropertyCategory.RESORT),
    SKI_RESORT(PropertyCategory.RESORT),
    GOLF_RESORT(PropertyCategory.RESORT),
    ECO_RESORT(PropertyCategory.RESORT),
    SPA_RESORT(PropertyCategory.RESORT),
    WELLNESS_RESORT(PropertyCategory.RESORT),
    DESERT_RESORT(PropertyCategory.RESORT),
    FOREST_RESORT(PropertyCategory.RESORT),
    FAMILY_RESORT(PropertyCategory.RESORT),
    ADVENTURE_RESORT(PropertyCategory.RESORT),

    // ── HOSTEL ───────────────────────────────────────────────────────────────
    HOSTEL(PropertyCategory.HOSTEL),
    BACKPACKER_HOSTEL(PropertyCategory.HOSTEL),
    YOUTH_HOSTEL(PropertyCategory.HOSTEL),
    STUDENT_HOSTEL(PropertyCategory.HOSTEL),
    CAPSULE_HOSTEL(PropertyCategory.HOSTEL),
    DORMITORY(PropertyCategory.HOSTEL),

    // ── GUEST_HOUSE_AND_BB ───────────────────────────────────────────────────
    GUEST_HOUSE(PropertyCategory.GUEST_HOUSE_AND_BB),
    BED_AND_BREAKFAST(PropertyCategory.GUEST_HOUSE_AND_BB),
    PENSION(PropertyCategory.GUEST_HOUSE_AND_BB),
    BOARDING_HOUSE(PropertyCategory.GUEST_HOUSE_AND_BB),
    HOMESTAY(PropertyCategory.GUEST_HOUSE_AND_BB),
    PRIVATE_ROOM(PropertyCategory.GUEST_HOUSE_AND_BB),

    // ── NATURE_AND_OUTDOOR ───────────────────────────────────────────────────
    ECO_LODGE(PropertyCategory.NATURE_AND_OUTDOOR),
    SAFARI_LODGE(PropertyCategory.NATURE_AND_OUTDOOR),
    JUNGLE_LODGE(PropertyCategory.NATURE_AND_OUTDOOR),
    FARM_STAY(PropertyCategory.NATURE_AND_OUTDOOR),
    AGRITOURISM_STAY(PropertyCategory.NATURE_AND_OUTDOOR),
    TREE_HOUSE(PropertyCategory.NATURE_AND_OUTDOOR),
    GLAMPING(PropertyCategory.NATURE_AND_OUTDOOR),
    TENT_CAMP(PropertyCategory.NATURE_AND_OUTDOOR),
    CARAVAN_PARK(PropertyCategory.NATURE_AND_OUTDOOR),
    HOLIDAY_PARK(PropertyCategory.NATURE_AND_OUTDOOR),

    // ── UNIQUE_STAY ──────────────────────────────────────────────────────────
    CASTLE(PropertyCategory.UNIQUE_STAY),
    CAVE_HOTEL(PropertyCategory.UNIQUE_STAY),
    ICE_HOTEL(PropertyCategory.UNIQUE_STAY),
    IGLOO(PropertyCategory.UNIQUE_STAY),
    HOUSEBOAT(PropertyCategory.UNIQUE_STAY),
    FLOATING_HOTEL(PropertyCategory.UNIQUE_STAY),
    YACHT(PropertyCategory.UNIQUE_STAY),
    LIGHTHOUSE(PropertyCategory.UNIQUE_STAY),
    MONASTERY_STAY(PropertyCategory.UNIQUE_STAY),
    TINY_HOUSE(PropertyCategory.UNIQUE_STAY);

    public final PropertyCategory category;

    PropertySubType(PropertyCategory category) {
        this.category = category;
    }
}
