package com.travolish.traveller.hosttools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoReplyTemplateRequest {
    @NotNull(message = "Host ID required")
    private Long hostId;
    
    @NotBlank(message = "Template name required")
    private String templateName;
    
    @NotNull(message = "Category required")
    private String category;
    
    @NotBlank(message = "Trigger keyword required")
    private String triggerKeyword;
    
    @NotBlank(message = "Template text required")
    private String templateText;
    
    private String language;
    
    private Integer displayOrder;
}
