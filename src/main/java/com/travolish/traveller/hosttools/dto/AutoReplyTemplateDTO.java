package com.travolish.traveller.hosttools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoReplyTemplateDTO {
    private Long id;
    private Long hostId;
    private String templateName;
    private String category;
    private String triggerKeyword;
    private String templateText;
    private Boolean isActive;
    private Integer usageCount;
    private String status;
    private String language;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
