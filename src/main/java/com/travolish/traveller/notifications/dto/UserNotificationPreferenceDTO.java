package com.travolish.traveller.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreferenceDTO {
    private Long userId;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean inAppEnabled;
    private Boolean bookingConfirmation;
    private Boolean bookingReminder;
    private Boolean checkInReminder;
    private Boolean checkOutReminder;
    private Boolean paymentNotifications;
    private Boolean promotionalOffers;
    private Boolean loyaltyUpdates;
    private Boolean reviewRequests;
    private Boolean accountAlerts;
    private Boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private String unsubscribedTypes;
}
