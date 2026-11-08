package com.travolish.traveller.listing.service;

import com.travolish.traveller.listing.dto.*;

public interface ListingWizardService {
    StepResponseDTO createDraft(Long hostId);
    StepResponseDTO saveStep1(Long draftId, Long hostId, Step1Request req);
    StepResponseDTO saveStep2(Long draftId, Long hostId, Step2Request req);
    StepResponseDTO saveStep3(Long draftId, Long hostId, Step3Request req);
    StepResponseDTO saveStep4(Long draftId, Long hostId, Step4Request req);
    ListingDraftDTO getDraft(Long draftId, Long hostId);
    Long publish(Long draftId, Long hostId);
    void abandonDraft(Long draftId, Long hostId);
}
