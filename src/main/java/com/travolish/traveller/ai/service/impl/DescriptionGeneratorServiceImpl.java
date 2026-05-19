package com.travolish.traveller.ai.service.impl;

import com.travolish.traveller.ai.dto.ListingDescriptionDTO;
import com.travolish.traveller.ai.dto.GenerateDescriptionRequest;
import com.travolish.traveller.ai.entity.ListingDescription;
import com.travolish.traveller.ai.repository.ListingDescriptionRepository;
import com.travolish.traveller.ai.service.DescriptionGeneratorService;
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
public class DescriptionGeneratorServiceImpl implements DescriptionGeneratorService {

    private final ListingDescriptionRepository listingDescriptionRepository;

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

    private String generateAIDescription(String original, String targetLanguage) {
        // Simulate AI description generation
        return "Enhanced description: " + original + " [Optimized for " + targetLanguage + "]";
    }

    private double calculateQualityScore(String original, String generated) {
        // Simple quality calculation based on length and content
        int originalLen = original.length();
        int generatedLen = generated.length();
        double lengthRatio = (double) generatedLen / Math.max(originalLen, 1);
        return Math.min(lengthRatio * 0.5 + 0.5, 1.0);
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
