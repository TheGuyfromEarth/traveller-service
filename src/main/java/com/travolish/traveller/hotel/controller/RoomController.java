package com.travolish.traveller.hotel.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * List rooms for a specific hotel.
     *
     * <p>{@code hotelId} is required. Fetching all rooms across every hotel in a single
     * request is prohibited — it dumps the full rooms table and causes severe performance
     * degradation on the search and home pages that previously called this without a filter.
     * Use GET /api/hotels/search for search (price is now included in the search response).
     */
    @GetMapping
    public List<Room> list(@RequestParam(value = "hotelId") Long hotelId) {
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "hotelId is required. Use GET /api/hotels/search for paginated hotel search.");
        }
        return roomService.findByHotelId(hotelId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> get(@PathVariable Long id) {
        return roomService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@Validated @RequestBody Room room) {
        return roomService.create(room);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> update(@PathVariable Long id, @Validated @RequestBody Room room) {
        return roomService.update(id, room).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
