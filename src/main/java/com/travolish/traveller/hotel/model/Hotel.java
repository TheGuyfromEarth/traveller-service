package com.travolish.traveller.hotel.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.travolish.traveller.review.model.Review;

@Entity
@Table(name = "hotels", indexes = {
    @Index(name = "idx_hotels_host_id",   columnList = "hostId"),
    @Index(name = "idx_hotels_status",    columnList = "status"),
    @Index(name = "idx_hotels_city",      columnList = "city"),
    @Index(name = "idx_hotels_country",   columnList = "country"),
    @Index(name = "idx_hotels_rating",    columnList = "rating"),
    @Index(name = "idx_hotels_lat_lng",   columnList = "latitude,longitude"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hostId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    // Optional at quick-create time; filled in via the Location tab in the listing editor
    private String state;

    private String country;

    private String postalCode;

    private Double rating;

    private String phone;

    private String email;

    @Column(length = 2000)
    private String description;

    // §5 — Basic property information
    // Step 1 of listing wizard — top-level category (replaces legacy PropertyType)
    @Enumerated(EnumType.STRING)
    private PropertyCategory category;

    // Step 2 — multi-select sub-types (e.g. LUXURY_HOTEL + SPA_HOTEL)
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_sub_types", joinColumns = @JoinColumn(name = "hotel_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_type")
    private List<PropertySubType> subTypes = new ArrayList<>();

    // Target guest segments (multi-select)
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_target_guests", joinColumns = @JoinColumn(name = "hotel_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "target_guest")
    private List<TargetGuest> targetGuests = new ArrayList<>();

    // Stay type
    @Enumerated(EnumType.STRING)
    private StayType stayType;

    private Integer starRating;

    // Step 3 — property dimensions
    private Integer numBedrooms;

    private Integer numBathrooms;

    private Integer maxGuests;

    private Integer numUnits;

    private String brand;

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_languages", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "language")
    private List<String> languagesSpoken = new ArrayList<>();

    private Integer yearBuilt;

    private Integer lastRenovated;

    // §6 — Location distances
    private String distanceToAirport;

    private String distanceToTrain;

    private String distanceToCityCentre;

    private String distanceToBeach;

    // §7 — Property details
    private Integer totalRooms;

    private Integer totalFloors;

    private Integer totalBuildings;

    private String propertySize;

    private String receptionHours;

    private Boolean twentyFourHourFrontDesk = false;

    // §13 — Photos & media
    private String imageUrl;

    private String coverPhotoTitle;

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_gallery_images", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "image_url")
    private List<String> galleryImages = new ArrayList<>();

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_drone_photos", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "photo_url")
    private List<String> dronePhotos = new ArrayList<>();

    private String videoUrl;

    private String threeSixtyTourUrl;

    // §6 — Geo
    private Double latitude;

    private Double longitude;

    @Column(length = 1000)
    private String houseRules;

    // §10 — Availability settings
    private Boolean instantBooking = true;

    private Integer minimumStay = 1;

    private Integer maximumStay;

    private Integer bookingWindow;

    private Boolean lastMinuteBooking = false;

    private Integer lastMinuteCutoffHours;

    private Boolean sameDayBooking = false;

    private String checkInTime;

    private String checkOutTime;

    // §11 — Amenities
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    // §14 — Meal options
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_meal_options", joinColumns = @JoinColumn(name = "hotel_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_option")
    private List<MealOption> mealOptions = new ArrayList<>();

    // §15 — Transportation
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_transportation", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "option")
    private List<String> transportationOptions = new ArrayList<>();

    // §16 — Guest services
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_guest_services", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "service")
    private List<String> guestServices = new ArrayList<>();

    // §17 — Sustainability
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_sustainability", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "feature")
    private List<String> sustainabilityFeatures = new ArrayList<>();

    // §18 — Contact
    private String contactPerson;

    private String websiteUrl;

    private String emergencyContact;

    // §19 — Bed details
    private String primaryBedType;

    private String secondaryBedType;

    // §20 — AI & SEO
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_target_audience", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "audience")
    private List<String> targetAudience = new ArrayList<>();

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_usp", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "usp")
    private List<String> usp = new ArrayList<>();

    private String nearbyLandmark;

    private Boolean aiTranslation = false;

    @Enumerated(EnumType.STRING)
    @Column
    private HotelStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    // §9 — Pricing (hotel-level defaults; rooms may override via Room.pricePerNight etc.)
    private Double weekdayPrice;

    private Double weekendPrice;

    private Double seasonalPrice;

    private Double holidayPrice;

    private Double weeklyDiscount;

    private Double monthlyDiscount;

    private Double taxes;

    private Double serviceCharges;

    private Double securityDeposit;

    @Column(length = 10)
    private String currency;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum HotelStatus {
        DRAFT,
        LIVE,
        PAUSED,
        PENDING_REVIEW
    }

    public enum MealOption {
        ROOM_ONLY,
        BREAKFAST,
        HALF_BOARD,
        FULL_BOARD,
        ALL_INCLUSIVE
    }

    @JsonIgnore
    @OneToMany(mappedBy = "hotelId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews;

}
