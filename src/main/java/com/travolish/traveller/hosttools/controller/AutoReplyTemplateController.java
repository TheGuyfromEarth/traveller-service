package com.travolish.traveller.hosttools.controller;

import com.travolish.traveller.hosttools.dto.AutoReplyTemplateDTO;
import com.travolish.traveller.hosttools.dto.AutoReplyTemplateRequest;
import com.travolish.traveller.hosttools.service.AutoReplyTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/host/auto-reply")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AutoReplyTemplateController {

    private final AutoReplyTemplateService autoReplyTemplateService;

    /**
     * Create auto-reply template
     * POST /api/host/auto-reply/create
     */
    @PostMapping("/create")
    public ResponseEntity<AutoReplyTemplateDTO> createTemplate(
            @Valid @RequestBody AutoReplyTemplateRequest request) {
        try {
            log.info("Creating auto-reply template for host: {}", request.getHostId());
            AutoReplyTemplateDTO template = autoReplyTemplateService.createTemplate(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(template);
        } catch (Exception e) {
            log.error("Error creating template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update auto-reply template
     * PUT /api/host/auto-reply/{templateId}
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<AutoReplyTemplateDTO> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody AutoReplyTemplateRequest request) {
        try {
            log.info("Updating template: {}", templateId);
            AutoReplyTemplateDTO template = autoReplyTemplateService.updateTemplate(templateId, request);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Error updating template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get template by ID
     * GET /api/host/auto-reply/{templateId}
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<AutoReplyTemplateDTO> getTemplateById(@PathVariable Long templateId) {
        try {
            AutoReplyTemplateDTO template = autoReplyTemplateService.getTemplateById(templateId);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get all templates for host
     * GET /api/host/auto-reply/host/{hostId}
     */
    @GetMapping("/host/{hostId}")
    public ResponseEntity<List<AutoReplyTemplateDTO>> getTemplatesForHost(
            @PathVariable Long hostId) {
        List<AutoReplyTemplateDTO> templates = autoReplyTemplateService.getTemplatesForHost(hostId);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates (paginated)
     * GET /api/host/auto-reply/host/{hostId}/paginated
     */
    @GetMapping("/host/{hostId}/paginated")
    public ResponseEntity<Page<AutoReplyTemplateDTO>> getTemplatesForHostPaginated(
            @PathVariable Long hostId,
            Pageable pageable) {
        Page<AutoReplyTemplateDTO> templates = autoReplyTemplateService.getTemplatesForHostPaginated(hostId, pageable);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get active templates for host
     * GET /api/host/auto-reply/host/{hostId}/active
     */
    @GetMapping("/host/{hostId}/active")
    public ResponseEntity<List<AutoReplyTemplateDTO>> getActiveTemplatesForHost(
            @PathVariable Long hostId) {
        List<AutoReplyTemplateDTO> templates = autoReplyTemplateService.getActiveTemplatesForHost(hostId);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates by category
     * GET /api/host/auto-reply/host/{hostId}/category/{category}
     */
    @GetMapping("/host/{hostId}/category/{category}")
    public ResponseEntity<List<AutoReplyTemplateDTO>> getTemplatesByCategory(
            @PathVariable Long hostId,
            @PathVariable String category) {
        List<AutoReplyTemplateDTO> templates = autoReplyTemplateService.getTemplatesByCategory(hostId, category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Delete template
     * DELETE /api/host/auto-reply/{templateId}
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<AutoReplyTemplateDTO> deleteTemplate(@PathVariable Long templateId) {
        try {
            log.info("Deleting template: {}", templateId);
            AutoReplyTemplateDTO template = autoReplyTemplateService.deleteTemplate(templateId);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate template
     * POST /api/host/auto-reply/{templateId}/activate
     */
    @PostMapping("/{templateId}/activate")
    public ResponseEntity<AutoReplyTemplateDTO> activateTemplate(@PathVariable Long templateId) {
        try {
            AutoReplyTemplateDTO template = autoReplyTemplateService.activateTemplate(templateId);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deactivate template
     * POST /api/host/auto-reply/{templateId}/deactivate
     */
    @PostMapping("/{templateId}/deactivate")
    public ResponseEntity<AutoReplyTemplateDTO> deactivateTemplate(@PathVariable Long templateId) {
        try {
            AutoReplyTemplateDTO template = autoReplyTemplateService.deactivateTemplate(templateId);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
