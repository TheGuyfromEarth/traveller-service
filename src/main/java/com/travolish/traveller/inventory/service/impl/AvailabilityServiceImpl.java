package com.travolish.traveller.inventory.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.inventory.dto.AvailabilityCheckDTO;
import com.travolish.traveller.inventory.exception.InsufficientAvailabilityException;
import com.travolish.traveller.inventory.exception.OverbookingException;
import com.travolish.traveller.inventory.model.RoomAvailability;
import com.travolish.traveller.inventory.repository.RoomAvailabilityRepository;
import com.travolish.traveller.inventory.service.AvailabilityService;
import com.travolish.traveller.hotel.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final RoomAvailabilityRepository availabilityRepository;
    private final RoomRepository roomRepository;

    @Override
    public Boolean isRoomAvailableOnDate(Long roomId, LocalDate date) {
        var availability = availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date);
        return availability.map(RoomAvailability::isAvailableForBooking).orElse(false);
    }

    @Override
    public Boolean isRoomAvailableForDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<RoomAvailability> availabilities = availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, checkInDate, checkOutDate.minusDays(1));
        
        // No records = no explicit blocks = room is available by default
        if (availabilities.isEmpty()) {
            return true;
        }

        return availabilities.stream().allMatch(RoomAvailability::isAvailableForBooking);
    }

    @Override
    public AvailabilityCheckDTO getAvailabilityForDate(Long roomId, LocalDate date) {
        return availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date)
            .map(this::convertToDTO)
            .orElseThrow(() -> InsufficientAvailabilityException.roomNotAvailable(roomId));
    }

    @Override
    public List<AvailabilityCheckDTO> getAvailabilityForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        
        return availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, checkInDate, checkOutDate.minusDays(1))
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AvailabilityCheckDTO> findAvailableRoomsOnDate(Long hotelId, LocalDate date) {
        return availabilityRepository.findAvailableRoomsOnDateAndHotelId(date, hotelId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> findAvailableRoomsInDateRange(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        long dayCount = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return availabilityRepository.findAvailableRoomsInDateRange(hotelId, checkInDate, checkOutDate.minusDays(1), dayCount);
    }

    @Override
    @Transactional
    public void bookRoom(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<RoomAvailability> existing = availabilityRepository
            .findByRoomIdAndDateRangeWithLock(roomId, checkInDate, checkOutDate.minusDays(1));

        Map<LocalDate, RoomAvailability> byDate = existing.stream()
            .collect(Collectors.toMap(RoomAvailability::getAvailabilityDate, a -> a));

        List<RoomAvailability> toBook = new ArrayList<>();
        for (LocalDate d = checkInDate; d.isBefore(checkOutDate); d = d.plusDays(1)) {
            RoomAvailability rec = byDate.get(d);
            if (rec == null) {
                rec = new RoomAvailability();
                rec.setRoomId(roomId);
                rec.setHotelId(hotelId);
                rec.setAvailabilityDate(d);
                rec.setTotalRooms(1);
                rec = availabilityRepository.save(rec);
            }
            if (!rec.isAvailableForBooking()) {
                throw OverbookingException.noRoomAvailable(roomId, null);
            }
            toBook.add(rec);
        }

        toBook.forEach(RoomAvailability::addBooking);
        availabilityRepository.saveAll(toBook);
    }

    @Override
    @Transactional
    public void cancelBooking(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<RoomAvailability> availabilities = availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, checkInDate, checkOutDate.minusDays(1));

        availabilities.forEach(RoomAvailability::removeBooking);
        availabilityRepository.saveAll(availabilities);
    }

    @Override
    @Transactional
    public void blockRoomsForMaintenance(Long roomId, LocalDate date, Integer count, String reason, Long hotelId) {
        var availability = availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date)
            .orElseGet(() -> availabilityRepository.save(
                RoomAvailability.builder()
                    .roomId(roomId)
                    .hotelId(hotelId != null ? hotelId : 0L)
                    .availabilityDate(date)
                    .totalRooms(1)
                    .bookedRooms(0)
                    .availableRooms(1)
                    .blockedRooms(0)
                    .build()
            ));
        availability.blockRooms(count, reason);
        availabilityRepository.save(availability);
    }

    @Override
    @Transactional
    public void unblockRooms(Long roomId, LocalDate date, Integer count, Long hotelId) {
        var availability = availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date)
            .orElseGet(() -> availabilityRepository.save(
                RoomAvailability.builder()
                    .roomId(roomId)
                    .hotelId(hotelId != null ? hotelId : 0L)
                    .availabilityDate(date)
                    .totalRooms(1)
                    .bookedRooms(0)
                    .availableRooms(1)
                    .blockedRooms(0)
                    .build()
            ));
        availability.unblockRooms(count);
        availabilityRepository.save(availability);
    }

    @Override
    @Transactional
    public void initializeRoomAvailability(Long hotelId, Long roomId, Integer roomCount, Integer daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead - 1);

        // Fetch existing dates in one query so we can skip them
        List<RoomAvailability> existing =
            availabilityRepository.findByRoomIdAndAvailabilityDateBetween(roomId, today, endDate);
        java.util.Set<LocalDate> alreadyExists = existing.stream()
            .map(RoomAvailability::getAvailabilityDate)
            .collect(Collectors.toSet());

        List<RoomAvailability> toCreate = new ArrayList<>();
        for (int i = 0; i < daysAhead; i++) {
            LocalDate date = today.plusDays(i);
            if (!alreadyExists.contains(date)) {
                toCreate.add(RoomAvailability.builder()
                    .roomId(roomId)
                    .hotelId(hotelId)
                    .availabilityDate(date)
                    .totalRooms(roomCount)
                    .bookedRooms(0)
                    .availableRooms(roomCount)
                    .blockedRooms(0)
                    .build());
            }
        }
        if (!toCreate.isEmpty()) {
            availabilityRepository.saveAll(toCreate);
        }
    }

    @Override
    public AvailabilityCheckDTO getHotelOccupancyOnDate(Long hotelId, LocalDate date) {
        var total = availabilityRepository.getTotalAvailableRoomsOnDate(hotelId, date);
        var booked = availabilityRepository.getTotalBookedRoomsOnDate(hotelId, date);

        int availCount = total.orElse(0);
        int bookedCount = booked.orElse(0);
        int totalCount = availCount + bookedCount;

        // No availability records for this date — rooms are open by default.
        // Fall back to the actual room count for the hotel.
        if (totalCount == 0) {
            totalCount = roomRepository.findByHotelId(hotelId).size();
            availCount = totalCount;
        }

        AvailabilityCheckDTO dto = new AvailabilityCheckDTO();
        dto.setHotelId(hotelId);
        dto.setAvailabilityDate(date);
        dto.setAvailableRooms(availCount);
        dto.setBookedRooms(bookedCount);
        dto.setTotalRooms(totalCount);

        if (dto.getTotalRooms() > 0) {
            dto.setOccupancyPercentage((double) dto.getBookedRooms() / dto.getTotalRooms() * 100);
        }

        return dto;
    }

    @Override
    public List<AvailabilityCheckDTO> getHotelOccupancyForDateRange(
        Long hotelId, LocalDate startDate, LocalDate endDate) {
        
        return availabilityRepository.findByHotelIdAndAvailabilityDateBetween(hotelId, startDate, endDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Double calculateAverageOccupancy(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return availabilityRepository.calculateAverageOccupancy(hotelId, startDate, endDate).orElse(0.0);
    }

    @Override
    public Boolean hasBookingConflict(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<RoomAvailability> availabilities = availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, checkInDate, checkOutDate.minusDays(1));

        return availabilities.stream()
            .anyMatch(a -> a.getAvailableRooms() == 0);
    }

    @Override
    @Transactional
    public void updateAvailabilityStatus(Long roomId, LocalDate date) {
        var availability = availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date)
            .orElse(null);

        if (availability != null) {
            availability.updateStatus();
            availabilityRepository.save(availability);
        }
    }

    @Override
    public List<AvailabilityCheckDTO> getRoomsWithLimitedAvailability(Long hotelId, LocalDate date) {
        return availabilityRepository
            .findByHotelIdAndAvailabilityDateAndStatus(hotelId, date, 
                RoomAvailability.AvailabilityStatus.LIMITED)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long archiveOldAvailabilityRecords(LocalDate beforeDate) {
        return availabilityRepository.deleteByAvailabilityDateBefore(beforeDate);
    }

    /**
     * Convert entity to DTO
     */
    private AvailabilityCheckDTO convertToDTO(RoomAvailability availability) {
        return AvailabilityCheckDTO.builder()
            .id(availability.getId())
            .roomId(availability.getRoomId())
            .hotelId(availability.getHotelId())
            .availabilityDate(availability.getAvailabilityDate())
            .totalRooms(availability.getTotalRooms())
            .bookedRooms(availability.getBookedRooms())
            .availableRooms(availability.getAvailableRooms())
            .blockedRooms(availability.getBlockedRooms())
            .status(availability.getStatus().toString())
            .occupancyPercentage(availability.getOccupancyPercentage())
            .availableForBooking(availability.isAvailableForBooking())
            .blockReason(availability.getBlockReason())
            .createdAt(availability.getCreatedAt())
            .updatedAt(availability.getUpdatedAt())
            .build();
    }
}
