package com.travolish.traveller.pricing.service.impl;

import com.travolish.traveller.pricing.dto.CompetitorAnalysisDTO;
import com.travolish.traveller.pricing.dto.DemandAnalysisDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.dto.SeasonalPricingDTO;
import com.travolish.traveller.pricing.entity.PricingSuggestion;
import com.travolish.traveller.pricing.repository.PricingSuggestionRepository;
import com.travolish.traveller.pricing.service.PricingAIService;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.repository.RoomRepository;
import com.travolish.traveller.inventory.model.RoomAvailability;
import com.travolish.traveller.inventory.repository.RoomAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingAIServiceImpl implements PricingAIService {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d");

    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final RoomAvailabilityRepository availabilityRepository;

    @Override
    public PricingSuggestionDTO generateSuggestion(PricingSuggestionRequest request) {
        log.info("Generating pricing suggestion for room: {}", request.getRoomId());

        BigDecimal suggestedPrice = calculateAISuggestedPrice(request.getRoomId(), request.getTargetOccupancyRate());
        BigDecimal currentPrice = roomRepository.findById(request.getRoomId())
                .map(r -> BigDecimal.valueOf(r.getPricePerNight() != null ? r.getPricePerNight() : 0.0))
                .orElse(BigDecimal.ZERO);
        BigDecimal priceChange = suggestedPrice.subtract(currentPrice);

        double confidenceScore = computeConfidenceScore(request.getRoomId());
        PricingSuggestion.SuggestionReason reason = determineSuggestionReason(request.getRoomId());
        PricingSuggestion.PricingTrend trend = determinePricingTrend(priceChange);

        PricingSuggestion suggestion = PricingSuggestion.builder()
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .suggestedPrice(suggestedPrice)
                .currentPrice(currentPrice)
                .priceChange(priceChange)
                .confidenceScore(confidenceScore)
                .reason(reason)
                .trend(trend)
                .suggestedFromDate(request.getFromDate())
                .suggestedToDate(request.getToDate())
                .analysis("AI-generated pricing analysis based on demand trends and market conditions")
                .status(PricingSuggestion.SuggestionStatus.PENDING)
                .occupancyRate(request.getTargetOccupancyRate())
                .build();

        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion generated with ID: {} and confidence: {}", saved.getId(), confidenceScore);

        return mapToDTO(saved);
    }

    @Override
    public List<PricingSuggestionDTO> generateSuggestionsForHotel(Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        if (rooms.isEmpty()) return List.of();
        LocalDate today = LocalDate.now();
        return rooms.stream().map(room -> {
            PricingSuggestionRequest req = PricingSuggestionRequest.builder()
                    .hotelId(hotelId)
                    .roomId(room.getId())
                    .fromDate(today)
                    .toDate(today.plusDays(30))
                    .build();
            return generateSuggestion(req);
        }).collect(Collectors.toList());
    }

    @Override
    public List<PricingSuggestionDTO> getSuggestionsForHotel(Long hotelId) {
        return pricingSuggestionRepository.findByHotelId(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingSuggestionDTO> getSuggestionsForRoom(Long roomId) {
        return pricingSuggestionRepository.findByRoomId(roomId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PricingSuggestionDTO> getPendingSuggestionsForHotel(Long hotelId, Pageable pageable) {
        return pricingSuggestionRepository.findByHotelIdAndStatus(
                hotelId, PricingSuggestion.SuggestionStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public PricingSuggestionDTO acceptSuggestion(Long suggestionId) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));

        // Apply the suggested price to the room, then mark APPLIED
        roomRepository.findById(suggestion.getRoomId()).ifPresent(room -> {
            room.setPricePerNight(suggestion.getSuggestedPrice().doubleValue());
            roomRepository.save(room);
            log.info("Applied suggested price {} to room {}", suggestion.getSuggestedPrice(), room.getId());
        });

        suggestion.setStatus(PricingSuggestion.SuggestionStatus.APPLIED);
        suggestion.setAcceptedAt(LocalDateTime.now());

        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion {} accepted and applied", suggestionId);

        return mapToDTO(saved);
    }

    @Override
    public PricingSuggestionDTO rejectSuggestion(Long suggestionId, String reason) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));

        suggestion.setStatus(PricingSuggestion.SuggestionStatus.REJECTED);
        suggestion.setRejectedAt(LocalDateTime.now());
        suggestion.setRejectionReason(reason);

        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion {} rejected: {}", suggestionId, reason);

        return mapToDTO(saved);
    }

    @Override
    public DemandAnalysisDTO analyzeDemandTrends(Long hotelId) {
        log.info("Analyzing demand trends for hotel: {}", hotelId);

        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.minusDays(7);
        LocalDate lastWeekStart = today.minusDays(14);

        // Booking velocity: compare confirmed bookings this week vs last week
        Long thisWeek = bookingRepository.countNonCancelledBookingsForHotelInPeriod(hotelId, thisWeekStart, today);
        Long lastWeek = bookingRepository.countNonCancelledBookingsForHotelInPeriod(hotelId, lastWeekStart, thisWeekStart);
        long tw = thisWeek != null ? thisWeek : 0L;
        long lw = lastWeek != null ? lastWeek : 0L;

        String searchTrend;
        if (lw == 0) {
            searchTrend = tw > 0 ? "New activity" : "No data yet";
        } else {
            long pct = ((tw - lw) * 100L) / lw;
            searchTrend = (pct >= 0 ? "+" : "") + pct + "% vs last week";
        }

        // Demand score: avg occupancy over next 30 days
        LocalDate next30 = today.plusDays(30);
        double occPct = availabilityRepository.calculateAverageOccupancy(hotelId, today, next30).orElse(50.0);

        String scoreNote;
        if (occPct > 70) scoreNote = "High demand — consider raising rates";
        else if (occPct < 40) scoreNote = "Low demand — consider promotional pricing";
        else scoreNote = "Moderate demand — rates are well-positioned";

        // Peak window: date with the highest projected occupancy in the next 30 days
        List<RoomAvailability> upcoming = availabilityRepository
                .findByHotelIdAndAvailabilityDateBetween(hotelId, today, next30);

        String peakWindow;
        String peakNote;
        LocalDate peakDate = upcoming.stream()
                .filter(a -> a.getTotalRooms() != null && a.getTotalRooms() > 0)
                .max(Comparator.comparingDouble(RoomAvailability::getOccupancyPercentage))
                .map(RoomAvailability::getAvailabilityDate)
                .orElse(null);

        if (peakDate != null) {
            peakWindow = peakDate.format(MONTH_DAY) + " – " + peakDate.plusDays(6).format(MONTH_DAY);
            peakNote = "Highest projected occupancy in the next 30 days";
        } else {
            peakWindow = "Insufficient data";
            peakNote = "Add room availability records to see projections";
        }

        return DemandAnalysisDTO.builder()
                .searchVolumeTrend(searchTrend)
                .searchNote("Based on confirmed booking activity")
                .bookingPace(tw + " booking" + (tw != 1 ? "s" : "") + " this week")
                .paceNote("Confirmed bookings in the last 7 days")
                .demandScore(String.format("%.0f%%", occPct))
                .scoreNote(scoreNote)
                .peakWindow(peakWindow)
                .peakNote(peakNote)
                .build();
    }

    @Override
    public CompetitorAnalysisDTO analyzeCompetitorPricing(Long hotelId) {
        log.info("Analyzing competitor pricing for hotel: {}", hotelId);

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        double ownAvgRate = rooms.stream()
                .filter(r -> r.getPricePerNight() != null && r.getPricePerNight() > 0)
                .mapToDouble(Room::getPricePerNight)
                .average()
                .orElse(0.0);

        if (ownAvgRate == 0.0) {
            return CompetitorAnalysisDTO.builder()
                    .avgRate("—")
                    .rateNote("No room pricing data available")
                    .yourPosition("—")
                    .positionNote("Set room prices to see market positioning")
                    .competitorOccupancy("—")
                    .occupancyNote("")
                    .priceGap("—")
                    .gapNote("")
                    .build();
        }

        // Use avg occupancy to estimate how the hotel is positioned in the market.
        // High occupancy at current rates → likely underpriced vs comp set.
        LocalDate today = LocalDate.now();
        double occPct = availabilityRepository
                .calculateAverageOccupancy(hotelId, today, today.plusDays(30))
                .orElse(50.0);

        // Comp set avg: if occupancy > 70% we're probably underpriced → comp set is higher;
        // if < 40% we're overpriced → comp set is lower.
        double marketMultiplier = occPct > 70 ? 1.12 : occPct > 50 ? 1.05 : 0.95;
        double compSetAvg = ownAvgRate * marketMultiplier;
        double priceGap = ownAvgRate - compSetAvg;

        String position;
        String positionNote;
        double gapPct = Math.abs(priceGap / compSetAvg) * 100;
        if (priceGap < -5) {
            position = String.format("%.0f%% below market", gapPct);
            positionNote = "Opportunity to raise rates and capture more revenue";
        } else if (priceGap > 5) {
            position = String.format("%.0f%% above market", gapPct);
            positionNote = "Premium positioning — ensure perceived value justifies the gap";
        } else {
            position = "At market rate";
            positionNote = "Your pricing is well-aligned with the comp set";
        }

        // Estimated competitor occupancy trends slightly opposite to own
        double compOcc = Math.min(occPct * (occPct > 60 ? 0.93 : 1.07), 100.0);

        return CompetitorAnalysisDTO.builder()
                .avgRate(String.format("$%.0f/night", compSetAvg))
                .rateNote("Estimated comp set average based on demand signals")
                .yourPosition(position)
                .positionNote(positionNote)
                .competitorOccupancy(String.format("%.0f%%", compOcc))
                .occupancyNote("Estimated competitor occupancy (next 30 days)")
                .priceGap(String.format("%s$%.0f", priceGap >= 0 ? "+" : "", priceGap))
                .gapNote("Your average rate vs estimated comp set")
                .build();
    }

    @Override
    public SeasonalPricingDTO generateSeasonalPricingSuggestions(Long hotelId) {
        log.info("Generating seasonal pricing suggestions for hotel: {}", hotelId);

        List<SeasonalPricingDTO.SeasonWindow> windows = List.of(
                SeasonalPricingDTO.SeasonWindow.builder()
                        .label("Winter Peak (Dec – Jan)")
                        .suggestedAdjustment("+15 – 20%")
                        .note("New Year holidays and winter leisure travel drive peak demand")
                        .build(),
                SeasonalPricingDTO.SeasonWindow.builder()
                        .label("Spring Shoulder (Feb – Mar)")
                        .suggestedAdjustment("+0 – 5%")
                        .note("Moderate travel; maintain competitive base rates")
                        .build(),
                SeasonalPricingDTO.SeasonWindow.builder()
                        .label("Summer Peak (May – Jun)")
                        .suggestedAdjustment("+10 – 15%")
                        .note("School holidays and leisure travel boost occupancy")
                        .build(),
                SeasonalPricingDTO.SeasonWindow.builder()
                        .label("Monsoon Low (Jul – Sep)")
                        .suggestedAdjustment("−10 – 15%")
                        .note("Reduced travel demand; promotional pricing improves occupancy")
                        .build(),
                SeasonalPricingDTO.SeasonWindow.builder()
                        .label("Festive Season (Oct – Nov)")
                        .suggestedAdjustment("+10%")
                        .note("Diwali and festive travel increase bookings significantly")
                        .build()
        );

        return SeasonalPricingDTO.builder().windows(windows).build();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * Computes the AI-suggested price for a room.
     *
     * 1. Fetch the room's base pricePerNight.
     * 2. If a targetOccupancyRate is supplied (from the request), use it directly;
     *    otherwise compute 30-day average from availability records.
     * 3. Apply occupancy-based demand multiplier.
     * 4. Apply a seasonal boost for known Indian peak months.
     */
    private BigDecimal calculateAISuggestedPrice(Long roomId, Integer targetOccupancyRate) {
        BigDecimal basePrice = roomRepository.findById(roomId)
                .map(r -> BigDecimal.valueOf(r.getPricePerNight() != null ? r.getPricePerNight() : 0.0))
                .orElse(BigDecimal.valueOf(100));

        if (basePrice.compareTo(BigDecimal.ZERO) == 0) basePrice = BigDecimal.valueOf(100);

        double occupancy;
        if (targetOccupancyRate != null) {
            occupancy = targetOccupancyRate;
        } else {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(30);
            OptionalDouble avg = availabilityRepository
                    .findByRoomIdAndAvailabilityDateBetween(roomId, start, end)
                    .stream()
                    .mapToDouble(a -> a.getOccupancyPercentage() != null ? a.getOccupancyPercentage() : 0.0)
                    .average();
            occupancy = avg.orElse(50.0);
        }

        double multiplier;
        if (occupancy > 80) multiplier = 1.20;
        else if (occupancy > 60) multiplier = 1.10;
        else if (occupancy > 40) multiplier = 1.00;
        else if (occupancy > 20) multiplier = 0.90;
        else multiplier = 0.80;

        int month = LocalDate.now().getMonthValue();
        if (month == 12 || month == 1 || month == 5 || month == 6) multiplier += 0.05;

        return basePrice.multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.HALF_UP);
    }

    private double computeConfidenceScore(Long roomId) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        long records = availabilityRepository
                .findByRoomIdAndAvailabilityDateBetween(roomId, start, end)
                .size();
        // More historical records → higher confidence, capped at 0.95
        return Math.min(0.40 + (records / 30.0) * 0.55, 0.95);
    }

    private PricingSuggestion.SuggestionReason determineSuggestionReason(Long roomId) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        OptionalDouble avg = availabilityRepository
                .findByRoomIdAndAvailabilityDateBetween(roomId, start, end)
                .stream()
                .mapToDouble(a -> a.getOccupancyPercentage() != null ? a.getOccupancyPercentage() : 0.0)
                .average();
        double occ = avg.orElse(50.0);

        if (occ > 75) return PricingSuggestion.SuggestionReason.HIGH_DEMAND;
        if (occ < 30) return PricingSuggestion.SuggestionReason.LOW_OCCUPANCY;
        int month = LocalDate.now().getMonthValue();
        if (month == 12 || month == 1 || month == 5 || month == 6)
            return PricingSuggestion.SuggestionReason.SEASONAL_TREND;
        return PricingSuggestion.SuggestionReason.MARKET_ADJUSTMENT;
    }

    private PricingSuggestion.PricingTrend determinePricingTrend(BigDecimal priceChange) {
        if (priceChange.signum() > 0) return PricingSuggestion.PricingTrend.INCREASE;
        if (priceChange.signum() < 0) return PricingSuggestion.PricingTrend.DECREASE;
        return PricingSuggestion.PricingTrend.STABLE;
    }

    private PricingSuggestionDTO mapToDTO(PricingSuggestion suggestion) {
        String roomName = roomRepository.findById(suggestion.getRoomId())
                .map(r -> r.getName() != null ? r.getName() : "Room " + r.getNumber())
                .orElse(null);

        return PricingSuggestionDTO.builder()
                .id(suggestion.getId())
                .hotelId(suggestion.getHotelId())
                .roomId(suggestion.getRoomId())
                .suggestedPrice(suggestion.getSuggestedPrice())
                .currentPrice(suggestion.getCurrentPrice())
                .priceChange(suggestion.getPriceChange())
                .confidenceScore(suggestion.getConfidenceScore())
                .reason(suggestion.getReason().toString())
                .trend(suggestion.getTrend().toString())
                .suggestedFromDate(suggestion.getSuggestedFromDate())
                .suggestedToDate(suggestion.getSuggestedToDate())
                .analysis(suggestion.getAnalysis())
                .status(suggestion.getStatus().toString())
                .occupancyRate(suggestion.getOccupancyRate())
                .competitorAvgPrice(suggestion.getCompetitorAvgPrice())
                .demandLevel(suggestion.getDemandLevel())
                .createdAt(suggestion.getCreatedAt())
                .roomName(roomName)
                .build();
    }
}
