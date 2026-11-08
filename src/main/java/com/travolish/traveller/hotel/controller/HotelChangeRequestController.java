package com.travolish.traveller.hotel.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.model.HotelChangeRequest;
import com.travolish.traveller.hotel.service.HotelChangeRequestService;

@RestController
@RequestMapping("/api/hotel-requests")
public class HotelChangeRequestController {

    private final HotelChangeRequestService service;

    public HotelChangeRequestController(HotelChangeRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelChangeRequest submit(@RequestBody HotelChangeRequest request) {
        return service.submit(request);
    }

    @GetMapping
    public List<HotelChangeRequest> list(@RequestParam(value = "status", required = false) HotelChangeRequest.RequestStatus status) {
        if (status == null) return service.findAll();
        return service.findByStatus(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelChangeRequest> get(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Admin approves
    @PostMapping("/{id}/approve")
    public ResponseEntity<HotelChangeRequest> approve(@PathVariable Long id, @RequestParam(value = "comment", required = false) String comment) {
        try {
            var updated = service.approve(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin rejects
    @PostMapping("/{id}/reject")
    public ResponseEntity<HotelChangeRequest> reject(@PathVariable Long id, @RequestParam(value = "comment", required = false) String comment) {
        try {
            var updated = service.reject(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
