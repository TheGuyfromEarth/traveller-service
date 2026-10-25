package com.travolish.traveller.hosttools.service.impl;

import com.travolish.traveller.hosttools.dto.AutoReplyTemplateDTO;
import com.travolish.traveller.hosttools.dto.AutoReplyTemplateRequest;
import com.travolish.traveller.hosttools.entity.AutoReplyTemplate;
import com.travolish.traveller.hosttools.repository.AutoReplyTemplateRepository;
import com.travolish.traveller.hosttools.service.AutoReplyTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutoReplyTemplateServiceImpl implements AutoReplyTemplateService {

    private final AutoReplyTemplateRepository autoReplyTemplateRepository;

    @Override
    public AutoReplyTemplateDTO createTemplate(AutoReplyTemplateRequest request) {
        log.info("Creating auto-reply template for host: {}", request.getHostId());

        AutoReplyTemplate template = AutoReplyTemplate.builder()
                .hostId(request.getHostId())
                .templateName(request.getTemplateName())
                .category(AutoReplyTemplate.TemplateCategory.valueOf(request.getCategory()))
                .triggerKeyword(request.getTriggerKeyword())
                .templateText(request.getTemplateText())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .status(AutoReplyTemplate.TemplateStatus.ACTIVE)
                .build();

        AutoReplyTemplate saved = autoReplyTemplateRepository.save(template);
        log.info("Template created with ID: {}", saved.getId());

        return mapToDTO(saved);
    }

    @Override
    public AutoReplyTemplateDTO updateTemplate(Long templateId, AutoReplyTemplateRequest request) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setTemplateName(request.getTemplateName());
        template.setCategory(AutoReplyTemplate.TemplateCategory.valueOf(request.getCategory()));
        template.setTriggerKeyword(request.getTriggerKeyword());
        template.setTemplateText(request.getTemplateText());
        if (request.getLanguage() != null) {
            template.setLanguage(request.getLanguage());
        }
        if (request.getDisplayOrder() != null) {
            template.setDisplayOrder(request.getDisplayOrder());
        }

        AutoReplyTemplate saved = autoReplyTemplateRepository.save(template);
        log.info("Template {} updated", templateId);

        return mapToDTO(saved);
    }

    @Override
    public AutoReplyTemplateDTO getTemplateById(Long templateId) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return mapToDTO(template);
    }

    @Override
    public List<AutoReplyTemplateDTO> getTemplatesForHost(Long hostId) {
        return autoReplyTemplateRepository.findByHostId(hostId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AutoReplyTemplateDTO> getTemplatesForHostPaginated(Long hostId, Pageable pageable) {
        return autoReplyTemplateRepository.findByHostId(hostId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<AutoReplyTemplateDTO> getActiveTemplatesForHost(Long hostId) {
        return autoReplyTemplateRepository.findActiveByHostId(hostId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AutoReplyTemplateDTO> getTemplatesByCategory(Long hostId, String category) {
        return autoReplyTemplateRepository.findActiveByHostIdAndCategory(hostId, category)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AutoReplyTemplateDTO findByKeyword(Long hostId, String keyword) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findByHostIdAndKeyword(hostId, keyword);
        if (template == null) {
            throw new RuntimeException("Template not found for keyword: " + keyword);
        }
        return mapToDTO(template);
    }

    @Override
    public AutoReplyTemplateDTO deleteTemplate(Long templateId) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setStatus(AutoReplyTemplate.TemplateStatus.ARCHIVED);
        template.setIsActive(false);

        AutoReplyTemplate saved = autoReplyTemplateRepository.save(template);
        log.info("Template {} archived", templateId);

        return mapToDTO(saved);
    }

    @Override
    public AutoReplyTemplateDTO activateTemplate(Long templateId) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setIsActive(true);
        template.setStatus(AutoReplyTemplate.TemplateStatus.ACTIVE);

        AutoReplyTemplate saved = autoReplyTemplateRepository.save(template);
        log.info("Template {} activated", templateId);

        return mapToDTO(saved);
    }

    @Override
    public AutoReplyTemplateDTO deactivateTemplate(Long templateId) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setIsActive(false);
        template.setStatus(AutoReplyTemplate.TemplateStatus.INACTIVE);

        AutoReplyTemplate saved = autoReplyTemplateRepository.save(template);
        log.info("Template {} deactivated", templateId);

        return mapToDTO(saved);
    }

    @Override
    public void incrementUsageCount(Long templateId) {
        AutoReplyTemplate template = autoReplyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsedAt(LocalDateTime.now());

        autoReplyTemplateRepository.save(template);
    }

    @Override
    public List<AutoReplyTemplateDTO> getDefaultTemplates() {
        // Return popular/default templates
        return List.of();
    }

    private AutoReplyTemplateDTO mapToDTO(AutoReplyTemplate template) {
        return AutoReplyTemplateDTO.builder()
                .id(template.getId())
                .hostId(template.getHostId())
                .templateName(template.getTemplateName())
                .category(template.getCategory().toString())
                .triggerKeyword(template.getTriggerKeyword())
                .templateText(template.getTemplateText())
                .isActive(template.getIsActive())
                .usageCount(template.getUsageCount())
                .status(template.getStatus().toString())
                .language(template.getLanguage())
                .displayOrder(template.getDisplayOrder())
                .createdAt(template.getCreatedAt())
                .lastUsedAt(template.getLastUsedAt())
                .build();
    }
}
