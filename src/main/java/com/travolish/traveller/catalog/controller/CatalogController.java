package com.travolish.traveller.catalog.controller;

import com.travolish.traveller.catalog.entity.CatalogItem;
import com.travolish.traveller.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@Slf4j
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<CatalogItem>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(catalogService.findAll(type, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogItem> getById(@PathVariable Long id) {
        return catalogService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItem create(@RequestBody CatalogItem item) {
        return catalogService.create(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogItem> update(@PathVariable Long id, @RequestBody CatalogItem item) {
        return catalogService.update(id, item)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CatalogItem> toggleStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return catalogService.toggleStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/order")
    public ResponseEntity<CatalogItem> updateOrder(
            @PathVariable Long id,
            @RequestParam Integer displayOrder) {
        return catalogService.updateOrder(id, displayOrder)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Long id) {
        boolean deleted = catalogService.delete(id);
        return deleted
                ? ResponseEntity.ok(Map.of("deleted", true))
                : ResponseEntity.notFound().build();
    }
}
