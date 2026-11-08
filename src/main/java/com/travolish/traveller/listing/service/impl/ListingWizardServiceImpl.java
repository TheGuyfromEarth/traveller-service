package com.travolish.traveller.listing.service.impl;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.travolish.traveller.hotel.model.*;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.listing.dto.*;
import com.travolish.traveller.listing.model.ListingDraft;
import com.travolish.traveller.listing.model.ListingDraft.DraftStatus;
import com.travolish.traveller.listing.repository.ListingDraftRepository;
import com.travolish.traveller.listing.service.ListingWizardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingWizardServiceImpl implements ListingWizardService {

    private final ListingDraftRepository draftRepo;
    private final HotelRepository hotelRepo;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public StepResponseDTO createDraft(Long hostId) {
        ListingDraft draft = ListingDraft.builder()
                .hostId(hostId)
                .build();
        draft = draftRepo.save(draft);
        return buildStepResponse(draft, 0, 1);
    }

    // ── Step saves ────────────────────────────────────────────────────────────

    @Override
    public StepResponseDTO saveStep1(Long draftId, Long hostId, Step1Request req) {
        ListingDraft draft = requireDraft(draftId, hostId);
        validateActive(draft);

        PropertyCategory cat = parseEnum(PropertyCategory.class, req.getCategory(), "category");
        draft.setCategory(cat);
        draft.setCurrentStep(Math.max(draft.getCurrentStep(), 2));
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = draftRepo.save(draft);
        return buildStepResponse(draft, 1, 2);
    }

    @Override
    public StepResponseDTO saveStep2(Long draftId, Long hostId, Step2Request req) {
        ListingDraft draft = requireDraft(draftId, hostId);
        validateActive(draft);
        validateCategorySet(draft);

        final PropertyCategory draftCategory = draft.getCategory();
        List<PropertySubType> subTypes = req.getSubTypes().stream()
                .map(s -> parseEnum(PropertySubType.class, s, "subType"))
                .filter(st -> st.category == draftCategory)
                .collect(Collectors.toList());

        if (subTypes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "None of the provided sub-types belong to category " + draft.getCategory());
        }

        draft.getSubTypes().clear();
        draft.getSubTypes().addAll(subTypes);
        draft.setCurrentStep(Math.max(draft.getCurrentStep(), 3));
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = draftRepo.save(draft);
        return buildStepResponse(draft, 2, 3);
    }

    @Override
    public StepResponseDTO saveStep3(Long draftId, Long hostId, Step3Request req) {
        ListingDraft draft = requireDraft(draftId, hostId);
        validateActive(draft);

        draft.setName(req.getName());
        draft.setStarRating(req.getStarRating());
        draft.setNumBedrooms(req.getNumBedrooms());
        draft.setNumBathrooms(req.getNumBathrooms());
        draft.setMaxGuests(req.getMaxGuests());
        draft.setNumUnits(req.getNumUnits());
        draft.setCheckInTime(req.getCheckInTime());
        draft.setCheckOutTime(req.getCheckOutTime());
        draft.setCurrentStep(Math.max(draft.getCurrentStep(), 4));
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = draftRepo.save(draft);
        return buildStepResponse(draft, 3, 4);
    }

    @Override
    public StepResponseDTO saveStep4(Long draftId, Long hostId, Step4Request req) {
        ListingDraft draft = requireDraft(draftId, hostId);
        validateActive(draft);

        StayType stayType = parseEnum(StayType.class, req.getStayType(), "stayType");

        draft.getAmenities().clear();
        if (req.getAmenities() != null) {
            draft.getAmenities().addAll(req.getAmenities());
        }

        draft.getTargetGuests().clear();
        if (req.getTargetGuests() != null) {
            List<TargetGuest> guests = req.getTargetGuests().stream()
                    .map(g -> parseEnum(TargetGuest.class, g, "targetGuest"))
                    .collect(Collectors.toList());
            draft.getTargetGuests().addAll(guests);
        }

        draft.setStayType(stayType);
        draft.setStatus(DraftStatus.READY_TO_PUBLISH);
        draft.setCurrentStep(Math.max(draft.getCurrentStep(), 4));
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = draftRepo.save(draft);
        return buildStepResponse(draft, 4, null);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ListingDraftDTO getDraft(Long draftId, Long hostId) {
        ListingDraft draft = requireDraft(draftId, hostId);
        return toDTO(draft);
    }

    // ── Publish (Phase G) ─────────────────────────────────────────────────────

    @Override
    public Long publish(Long draftId, Long hostId) {
        ListingDraft draft = requireDraft(draftId, hostId);

        if (draft.getStatus() != DraftStatus.READY_TO_PUBLISH
                && draft.getStatus() != DraftStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Draft cannot be published — current status: " + draft.getStatus());
        }
        validatePublishReady(draft);

        Hotel hotel = new Hotel();
        hotel.setHostId(hostId);
        hotel.setName(draft.getName());
        hotel.setCategory(draft.getCategory());
        hotel.setSubTypes(new java.util.ArrayList<>(draft.getSubTypes()));
        hotel.setTargetGuests(new java.util.ArrayList<>(draft.getTargetGuests()));
        hotel.setStayType(draft.getStayType());
        hotel.setStarRating(draft.getStarRating());
        hotel.setNumBedrooms(draft.getNumBedrooms());
        hotel.setNumBathrooms(draft.getNumBathrooms());
        hotel.setMaxGuests(draft.getMaxGuests());
        hotel.setNumUnits(draft.getNumUnits());
        hotel.setCheckInTime(draft.getCheckInTime());
        hotel.setCheckOutTime(draft.getCheckOutTime());
        hotel.setAmenities(new java.util.ArrayList<>(draft.getAmenities()));
        hotel.setStatus(Hotel.HotelStatus.DRAFT);

        hotel = hotelRepo.save(hotel);

        draft.setPublishedHotelId(hotel.getId());
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftRepo.save(draft);

        return hotel.getId();
    }

    // ── Abandon ───────────────────────────────────────────────────────────────

    @Override
    public void abandonDraft(Long draftId, Long hostId) {
        ListingDraft draft = requireDraft(draftId, hostId);
        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Published drafts cannot be abandoned");
        }
        draft.setStatus(DraftStatus.ABANDONED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftRepo.save(draft);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ListingDraft requireDraft(Long draftId, Long hostId) {
        return draftRepo.findByIdAndHostId(draftId, hostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Draft not found: " + draftId));
    }

    private void validateActive(ListingDraft draft) {
        if (draft.getStatus() == DraftStatus.ABANDONED || draft.getStatus() == DraftStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Draft is no longer active: " + draft.getStatus());
        }
        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Draft has already been published");
        }
        if (draft.getExpiresAt() != null && OffsetDateTime.now().isAfter(draft.getExpiresAt())) {
            draft.setStatus(DraftStatus.EXPIRED);
            draftRepo.save(draft);
            throw new ResponseStatusException(HttpStatus.GONE, "Draft has expired");
        }
    }

    private void validateCategorySet(ListingDraft draft) {
        if (draft.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete step 1 (select a category) before step 2");
        }
    }

    private void validatePublishReady(ListingDraft draft) {
        if (draft.getName() == null || draft.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Property name is required to publish");
        }
        if (draft.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Category is required to publish");
        }
        if (draft.getMaxGuests() == null || draft.getMaxGuests() < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Max guests must be at least 1 to publish");
        }
        if (draft.getNumUnits() == null || draft.getNumUnits() < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Number of units must be at least 1 to publish");
        }
        if (draft.getStayType() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Stay type is required to publish");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        try {
            return Enum.valueOf(type, value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            String valid = Arrays.stream(type.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + " '" + value + "'. Valid values: " + valid);
        }
    }

    private StepResponseDTO buildStepResponse(ListingDraft draft, int completedStep, Integer nextStep) {
        return StepResponseDTO.builder()
                .draftId(draft.getId())
                .completedStep(completedStep)
                .nextStep(nextStep)
                .wizardComplete(nextStep == null)
                .nextStepFields(fieldsForStep(nextStep))
                .draft(toDTO(draft))
                .build();
    }

    private List<String> fieldsForStep(Integer step) {
        if (step == null) return List.of();
        return switch (step) {
            case 1 -> List.of("category");
            case 2 -> List.of("subTypes");
            case 3 -> List.of("name", "starRating", "numBedrooms", "numBathrooms",
                              "maxGuests", "numUnits", "checkInTime", "checkOutTime");
            case 4 -> List.of("amenities", "targetGuests", "stayType");
            default -> List.of();
        };
    }

    private ListingDraftDTO toDTO(ListingDraft d) {
        return ListingDraftDTO.builder()
                .id(d.getId())
                .hostId(d.getHostId())
                .currentStep(d.getCurrentStep())
                .status(d.getStatus())
                .category(d.getCategory() != null ? d.getCategory().name() : null)
                .subTypes(d.getSubTypes().stream().map(Enum::name).collect(Collectors.toList()))
                .name(d.getName())
                .starRating(d.getStarRating())
                .numBedrooms(d.getNumBedrooms())
                .numBathrooms(d.getNumBathrooms())
                .maxGuests(d.getMaxGuests())
                .numUnits(d.getNumUnits())
                .checkInTime(d.getCheckInTime())
                .checkOutTime(d.getCheckOutTime())
                .amenities(d.getAmenities())
                .targetGuests(d.getTargetGuests().stream().map(Enum::name).collect(Collectors.toList()))
                .stayType(d.getStayType() != null ? d.getStayType().name() : null)
                .publishedHotelId(d.getPublishedHotelId())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .expiresAt(d.getExpiresAt())
                .build();
    }
}
