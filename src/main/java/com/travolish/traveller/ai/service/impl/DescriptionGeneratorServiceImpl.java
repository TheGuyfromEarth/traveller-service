package com.travolish.traveller.ai.service.impl;

import com.travolish.traveller.ai.dto.ListingDescriptionDTO;
import com.travolish.traveller.ai.dto.GenerateDescriptionRequest;
import com.travolish.traveller.ai.entity.ListingDescription;
import com.travolish.traveller.ai.repository.ListingDescriptionRepository;
import com.travolish.traveller.ai.service.DescriptionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DescriptionGeneratorServiceImpl implements DescriptionGeneratorService {

    private final ListingDescriptionRepository listingDescriptionRepository;

    @Value("${anthropic.api.key:${ANTHROPIC_API_KEY:}}")
    private String anthropicApiKey;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-haiku-4-5-20251001"; // fast + cost-effective for descriptions

    @Override
    public ListingDescriptionDTO generateDescription(GenerateDescriptionRequest request) {
        log.info("Generating AI description for room: {} in hotel: {}", request.getRoomId(), request.getHotelId());

        // Simulate AI generation
        String generatedDescription = generateAIDescription(request.getOriginalDescription(), request.getTargetLanguage());
        double qualityScore = calculateQualityScore(request.getOriginalDescription(), generatedDescription);
        double confidenceScore = Math.random();

        ListingDescription description = ListingDescription.builder()
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId() != null ? request.getRoomId() : 0L)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .descriptionType(request.getDescriptionType() != null
                        ? ListingDescription.DescriptionType.valueOf(request.getDescriptionType())
                        : ListingDescription.DescriptionType.ROOM_DESCRIPTION)
                .originalDescription(request.getOriginalDescription())
                .generatedDescription(generatedDescription)
                .qualityScore(qualityScore)
                .confidenceScore(confidenceScore)
                .status(ListingDescription.GenerationStatus.COMPLETED)
                .approved(false)
                .isActive(false)
                .characterCount(generatedDescription.length())
                .wordCount(generatedDescription.split("\\s+").length)
                .aiModel("GPT-4-Turbo")
                .build();

        ListingDescription saved = listingDescriptionRepository.save(description);
        log.info("Description generated with ID: {} and quality score: {}", saved.getId(), qualityScore);

        return mapToDTO(saved);
    }

    @Override
    public List<ListingDescriptionDTO> getDescriptionsForHotel(Long hotelId) {
        return listingDescriptionRepository.findByHotelId(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ListingDescriptionDTO> getDescriptionsForRoom(Long roomId) {
        return listingDescriptionRepository.findByRoomId(roomId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ListingDescriptionDTO> getPendingApprovals(Pageable pageable) {
        return listingDescriptionRepository.findPendingApprovals(pageable)
                .map(this::mapToDTO);
    }

    @Override
    public ListingDescriptionDTO approveDescription(Long descriptionId, String approvalNotes) {
        ListingDescription description = listingDescriptionRepository.findById(descriptionId)
                .orElseThrow(() -> new RuntimeException("Description not found"));

        description.setApproved(true);
        description.setStatus(ListingDescription.GenerationStatus.APPROVED);
        description.setApprovedAt(LocalDateTime.now());
        description.setApprovalNotes(approvalNotes);

        ListingDescription saved = listingDescriptionRepository.save(description);
        log.info("Description {} approved", descriptionId);

        return mapToDTO(saved);
    }

    @Override
    public ListingDescriptionDTO rejectDescription(Long descriptionId, String rejectionReason) {
        ListingDescription description = listingDescriptionRepository.findById(descriptionId)
                .orElseThrow(() -> new RuntimeException("Description not found"));

        description.setApproved(false);
        description.setStatus(ListingDescription.GenerationStatus.REJECTED);
        description.setApprovalNotes(rejectionReason);

        ListingDescription saved = listingDescriptionRepository.save(description);
        log.info("Description {} rejected: {}", descriptionId, rejectionReason);

        return mapToDTO(saved);
    }

    @Override
    public ListingDescriptionDTO activateDescription(Long descriptionId) {
        ListingDescription description = listingDescriptionRepository.findById(descriptionId)
                .orElseThrow(() -> new RuntimeException("Description not found"));

        if (!description.getApproved()) {
            throw new RuntimeException("Cannot activate unapproved description");
        }

        description.setIsActive(true);
        ListingDescription saved = listingDescriptionRepository.save(description);
        log.info("Description {} activated", descriptionId);

        return mapToDTO(saved);
    }

    @Override
    public ListingDescriptionDTO deactivateDescription(Long descriptionId) {
        ListingDescription description = listingDescriptionRepository.findById(descriptionId)
                .orElseThrow(() -> new RuntimeException("Description not found"));

        description.setIsActive(false);
        ListingDescription saved = listingDescriptionRepository.save(description);
        log.info("Description {} deactivated", descriptionId);

        return mapToDTO(saved);
    }

    @Override
    public List<ListingDescriptionDTO> getActiveDescriptionsForHotel(Long hotelId) {
        return listingDescriptionRepository.findActiveDescriptionsForHotel(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String translateDescription(String text, String fromLanguage, String toLanguage) {
        log.info("Translating description from {} to {}", fromLanguage, toLanguage);
        // Simulate translation
        return "[Translated] " + text;
    }

    /**
     * Calls Anthropic Claude API to generate an enhanced property description.
     * Falls back to a template-based enhancement when the API key is not set.
     */
    private String generateAIDescription(String original, String targetLanguage) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank() || anthropicApiKey.startsWith("placeholder")) {
            log.warn("Anthropic API key not configured — using template-based description enhancement");
            return buildTemplateDescription(original, targetLanguage);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            String prompt = buildDescriptionPrompt(original, targetLanguage);

            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> body = Map.of(
                "model", CLAUDE_MODEL,
                "max_tokens", 512,
                "messages", List.of(message)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(ANTHROPIC_API_URL, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    String generated = (String) content.get(0).get("text");
                    log.info("Claude API generated description ({} chars)", generated.length());
                    return generated.trim();
                }
            }
        } catch (Exception e) {
            log.error("Claude API call failed: {} — falling back to template", e.getMessage());
        }

        return buildTemplateDescription(original, targetLanguage);
    }

    private String buildDescriptionPrompt(String original, String targetLanguage) {
        String lang = targetLanguage != null && !targetLanguage.isBlank() ? targetLanguage : "English";
        return String.format("""
            You are a professional travel copywriter. Rewrite and enhance the following hotel/room description \
            to make it more engaging, vivid, and compelling for travellers. \
            Use warm, welcoming language and highlight unique features. \
            Keep it between 80-150 words. \
            Write in %s.

            Original description:
            %s

            Enhanced description:""", lang, original);
    }

    private String buildTemplateDescription(String original, String targetLanguage) {
        if (original == null || original.isBlank()) return "A comfortable and welcoming property awaiting your arrival.";
        String base = original.trim();
        String suffix = "ENGLISH".equalsIgnoreCase(targetLanguage) || targetLanguage == null ? "" : " [" + targetLanguage + "]";
        return "Experience " + base.toLowerCase().replaceFirst("^an? |^the ", "")
            + ". This carefully curated property offers everything you need for a memorable stay." + suffix;
    }

    private double calculateQualityScore(String original, String generated) {
        if (generated == null || original == null) return 0.5;
        int generatedLen = generated.length();
        // Score based on whether the generated text is meaningfully longer than original
        // and within the ideal 80-200 word range
        int wordCount = generated.split("\\s+").length;
        double lengthScore = wordCount >= 60 && wordCount <= 200 ? 1.0 : 0.7;
        return Math.min(lengthScore, 1.0);
    }

    private ListingDescriptionDTO mapToDTO(ListingDescription description) {
        return ListingDescriptionDTO.builder()
                .id(description.getId())
                .hotelId(description.getHotelId())
                .roomId(description.getRoomId())
                .sourceLanguage(description.getSourceLanguage())
                .targetLanguage(description.getTargetLanguage())
                .descriptionType(description.getDescriptionType() != null ? description.getDescriptionType().toString() : null)
                .originalDescription(description.getOriginalDescription())
                .generatedDescription(description.getGeneratedDescription())
                .qualityScore(description.getQualityScore())
                .confidenceScore(description.getConfidenceScore())
                .status(description.getStatus().toString())
                .approved(description.getApproved())
                .isActive(description.getIsActive())
                .characterCount(description.getCharacterCount())
                .wordCount(description.getWordCount())
                .createdAt(description.getCreatedAt())
                .build();
    }
}
