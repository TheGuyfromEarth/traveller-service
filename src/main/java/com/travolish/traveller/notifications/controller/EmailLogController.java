package com.travolish.traveller.notifications.controller;

import com.travolish.traveller.notifications.entity.EmailLog;
import com.travolish.traveller.notifications.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogRepository emailLogRepository;

    @GetMapping
    public ResponseEntity<Page<EmailLog>> getLogs(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "50") int    size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String recipient
    ) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("sentAt").descending());

        boolean hasStatus    = status    != null && !status.isBlank();
        boolean hasRecipient = recipient != null && !recipient.isBlank();

        Page<EmailLog> result;
        if (hasStatus && hasRecipient) {
            result = emailLogRepository.findByStatusAndRecipientContainingIgnoreCase(
                    status.toUpperCase(), recipient, pageable);
        } else if (hasStatus) {
            result = emailLogRepository.findByStatus(status.toUpperCase(), pageable);
        } else if (hasRecipient) {
            result = emailLogRepository.findByRecipientContainingIgnoreCase(recipient, pageable);
        } else {
            result = emailLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(Map.of(
                "total",   emailLogRepository.count(),
                "sent",    emailLogRepository.countByStatus("SENT"),
                "failed",  emailLogRepository.countByStatus("FAILED"),
                "skipped", emailLogRepository.countByStatus("SKIPPED")
        ));
    }
}
