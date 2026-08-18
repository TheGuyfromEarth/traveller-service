package com.travolish.traveller.hosttools.service;

import com.travolish.traveller.hosttools.dto.AutoReplyTemplateDTO;
import com.travolish.traveller.hosttools.dto.AutoReplyTemplateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AutoReplyTemplateService {
    AutoReplyTemplateDTO createTemplate(AutoReplyTemplateRequest request);
    
    AutoReplyTemplateDTO updateTemplate(Long templateId, AutoReplyTemplateRequest request);
    
    AutoReplyTemplateDTO getTemplateById(Long templateId);
    
    List<AutoReplyTemplateDTO> getTemplatesForHost(Long hostId);
    
    Page<AutoReplyTemplateDTO> getTemplatesForHostPaginated(Long hostId, Pageable pageable);
    
    List<AutoReplyTemplateDTO> getActiveTemplatesForHost(Long hostId);
    
    List<AutoReplyTemplateDTO> getTemplatesByCategory(Long hostId, String category);
    
    AutoReplyTemplateDTO findByKeyword(Long hostId, String keyword);
    
    AutoReplyTemplateDTO deleteTemplate(Long templateId);
    
    AutoReplyTemplateDTO activateTemplate(Long templateId);
    
    AutoReplyTemplateDTO deactivateTemplate(Long templateId);
    
    void incrementUsageCount(Long templateId);
    
    List<AutoReplyTemplateDTO> getDefaultTemplates();
}
