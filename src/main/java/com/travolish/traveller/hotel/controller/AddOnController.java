package com.travolish.traveller.hotel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.hotel.dto.AddOnDTO;
import com.travolish.traveller.hotel.service.AddOnService;

@RestController
@RequestMapping("/api/addons")
public class AddOnController {

    private final AddOnService addOnService;

    public AddOnController(AddOnService addOnService) {
        this.addOnService = addOnService;
    }

    @GetMapping
    public ResponseEntity<List<AddOnDTO>> getByHotelId(@RequestParam Long hotelId) {
        return ResponseEntity.ok(addOnService.findByHotelId(hotelId));
    }

    @PostMapping
    public ResponseEntity<AddOnDTO> create(@RequestBody AddOnDTO dto) {
        return ResponseEntity.ok(addOnService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddOnDTO> update(@PathVariable Long id, @RequestBody AddOnDTO dto) {
        return ResponseEntity.ok(addOnService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addOnService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
