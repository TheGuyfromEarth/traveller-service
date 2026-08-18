package com.travolish.traveller.admin.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository repository;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasSearch = search != null && !search.isBlank();

        if (entityType != null && entityId != null) {
            List<AuditLog> logs = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
            return ResponseEntity.ok(new PageImpl<>(logs, pageable, logs.size()));
        }

        if (hasSearch && entityType != null) {
            return ResponseEntity.ok(repository.searchByEntityType(entityType, search, pageable));
        }
        if (hasSearch) {
            return ResponseEntity.ok(repository.searchAll(search, pageable));
        }
        if (entityType != null) {
            return ResponseEntity.ok(repository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable));
        }
        return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditLog createLog(@RequestBody AuditLog entry) {
        entry.setId(null);
        return repository.save(entry);
    }
}
