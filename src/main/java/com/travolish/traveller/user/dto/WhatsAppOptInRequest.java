package com.travolish.traveller.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WhatsAppOptInRequest {
    private String phone;
    @NotNull
    private Boolean whatsappOptIn;
}
