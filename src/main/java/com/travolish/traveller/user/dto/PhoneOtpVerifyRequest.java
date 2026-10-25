package com.travolish.traveller.user.dto;

import lombok.Data;

@Data
public class PhoneOtpVerifyRequest {
    private String phone;
    private String code;
}
