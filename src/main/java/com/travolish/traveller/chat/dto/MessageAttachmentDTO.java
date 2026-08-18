package com.travolish.traveller.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentDTO {
    private Long id;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
}
