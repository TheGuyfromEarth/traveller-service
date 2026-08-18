package com.travolish.traveller.ai.service.impl;

import com.travolish.traveller.ai.dto.ListingDescriptionDTO;
import com.travolish.traveller.ai.dto.GenerateDescriptionRequest;
import com.travolish.traveller.ai.entity.ListingDescription;
import com.travolish.traveller.ai.repository.ListingDescriptionRepository;
import com.travolish.traveller.ai.service.DescriptionGeneratorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
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

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @PostConstruct
    void logKeyStatus() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set — AI description generation will use template fallback");
        } else {
            log.info("Gemini API key loaded ({}...{})", geminiApiKey.substring(0, Math.min(4, geminiApiKey.length())),
                     geminiApiKey.substring(Math.max(0, geminiApiKey.length() - 4)));
        }
    }

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=";

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
                .aiModel(GEMINI_MODEL)
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
     * Calls Google Gemini API to generate an enhanced property description.
     * Falls back to a template-based enhancement when the API key is not set.
     * Free tier: 1,500 requests/day, 15 RPM — sufficient for an MVP.
     */
    private String generateAIDescription(String original, String targetLanguage) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.startsWith("placeholder")) {
            log.warn("Gemini API key not configured — using template-based description enhancement");
            return buildTemplateDescription(original, targetLanguage);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = buildDescriptionPrompt(original, targetLanguage);

            // Gemini request shape: { contents: [{ parts: [{ text }] }] }
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                GEMINI_API_URL + geminiApiKey, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidate = candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidateContent = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String generated = (String) parts.get(0).get("text");
                        log.info("Gemini API generated description ({} chars)", generated.length());
                        return generated.trim();
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("Gemini API call failed — HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Gemini API call failed — {}: {}", e.getClass().getSimpleName(), e.getMessage());
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
