package com.travolish.traveller.listing.dto;

import java.util.List;

import lombok.*;

/** Returned after every step PUT — tells the frontend what step was saved and what comes next. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResponseDTO {
    private Long draftId;
    private Integer completedStep;
    private Integer nextStep;
    private boolean wizardComplete;
    private List<String> nextStepFields;
    private ListingDraftDTO draft;
}
