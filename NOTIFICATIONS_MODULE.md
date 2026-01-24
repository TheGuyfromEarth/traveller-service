# Notifications & Communications Module

## Overview

The Notifications & Communications module provides a comprehensive system for sending and managing notifications to users through multiple channels: Email, SMS, and In-App notifications. The system supports templates, scheduling, user preferences, and automatic retry mechanisms.

---

## 📊 Module Architecture

### Components

1. **Entities**
   - `Notification` - Individual notification records
   - `NotificationTemplate` - Reusable notification templates
   - `UserNotificationPreference` - User preference settings
   - Enums: `NotificationType`, `NotificationChannel`, `NotificationStatus`

2. **Services**
   - `NotificationService` - Core business logic
   - `EmailService` - Email sending via SMTP
   - `SmsService` - SMS sending (Twilio/AWS SNS ready)

3. **Controller**
   - `NotificationController` - REST endpoints

4. **Database**
   - Repositories with optimized indexes
   - Support for pagination and filtering

---

## 🔧 Features

### Notification Channels
- **EMAIL** - SMTP-based email notifications
- **SMS** - SMS notifications via Twilio or AWS SNS
- **IN_APP** - In-application notifications stored in database

### Notification Types
- `BOOKING_CONFIRMATION` - Booking confirmed
- `BOOKING_REMINDER` - Reminder before check-in
- `BOOKING_CANCELLATION` - Booking cancelled
- `BOOKING_MODIFIED` - Booking details changed
- `CHECK_IN_REMINDER` - Day of check-in reminder
- `CHECK_OUT_REMINDER` - Day of check-out reminder
- `PAYMENT_RECEIVED` - Payment confirmation
- `PAYMENT_FAILED` - Payment failure notification
- `SPECIAL_REQUEST_CONFIRMATION` - Special request confirmed
- `REVIEW_REQUEST` - Request to leave review
- `PROMOTIONAL_OFFER` - Special offers
- `LOYALTY_POINTS_EARNED` - Loyalty rewards
- `PASSWORD_RESET` - Password reset request
- `EMAIL_VERIFICATION` - Email verification link
- `ACCOUNT_ALERT` - Account security alerts
- `MAINTENANCE_NOTICE` - Maintenance announcements
- `GENERAL_ANNOUNCEMENT` - General messages

### Key Features
✅ Template-based notifications
✅ Scheduled sending
✅ Automatic retry mechanism (configurable)
✅ User preference management
✅ Unread notification tracking
✅ Quiet hours support
✅ Bulk notification support
✅ Notification history and analytics

---

## 📡 API Endpoints

### 1. Send Email Notification
```http
POST /api/notifications/email
Content-Type: application/json

{
  "userId": 1,
  "type": "BOOKING_CONFIRMATION",
  "channel": "EMAIL",
  "recipientEmail": "guest@example.com",
  "subject": "Booking Confirmation - Order #12345",
  "message": "Your booking has been confirmed...",
  "bookingId": 123,
  "hotelId": 456,
  "sendImmediately": true
}

Response: 201 Created
{
  "id": 1,
  "userId": 1,
  "type": "BOOKING_CONFIRMATION",
  "channel": "EMAIL",
  "status": "SENT",
  "sentTime": "2024-12-27T10:30:00",
  "createdAt": "2024-12-27T10:30:00"
}
```

### 2. Send SMS Notification
```http
POST /api/notifications/sms
Content-Type: application/json

{
  "userId": 1,
  "type": "CHECK_IN_REMINDER",
  "channel": "SMS",
  "recipientPhone": "+1234567890",
  "message": "Reminder: Your check-in is today at 3 PM",
  "bookingId": 123,
  "sendImmediately": true
}

Response: 201 Created
```

### 3. Get Notification Templates
```http
GET /api/notifications/templates
Response: 200 OK
[
  {
    "id": 1,
    "type": "BOOKING_CONFIRMATION",
    "name": "Booking Confirmation Email",
    "channel": "EMAIL",
    "subjectTemplate": "Booking Confirmation - {bookingId}",
    "messageTemplate": "Dear {guestName}, your booking...",
    "htmlTemplate": "<html>...</html>",
    "isActive": true
  }
]
```

### 4. Get Active Templates
```http
GET /api/notifications/templates/active
Response: 200 OK
```

### 5. Schedule Notification
```http
POST /api/notifications/schedule
Content-Type: application/json

{
  "userId": 1,
  "type": "CHECK_IN_REMINDER",
  "channel": "EMAIL",
  "recipientEmail": "guest@example.com",
  "subject": "Check-in Reminder",
  "message": "Your check-in is tomorrow",
  "scheduledTime": "2024-12-28T14:00:00",
  "sendImmediately": false
}

Response: 201 Created
{
  "id": 2,
  "status": "SCHEDULED",
  "scheduledTime": "2024-12-28T14:00:00"
}
```

### 6. Get User Notifications
```http
GET /api/notifications/user/1?page=0&size=10
Response: 200 OK
{
  "content": [...],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 10
}
```

### 7. Get Unread Notifications
```http
GET /api/notifications/user/1/unread?page=0&size=10
Response: 200 OK
```

### 8. Get Unread Count
```http
GET /api/notifications/user/1/unread-count
Response: 200 OK
5
```

### 9. Mark Notification as Read
```http
POST /api/notifications/1/read
Response: 200 OK
{
  "id": 1,
  "isRead": true,
  "readTime": "2024-12-27T11:00:00"
}
```

### 10. Delete Notification
```http
DELETE /api/notifications/1
Response: 204 No Content
```

### 11. Get User Preferences
```http
GET /api/notifications/preferences/user/1
Response: 200 OK
{
  "userId": 1,
  "emailEnabled": true,
  "smsEnabled": false,
  "inAppEnabled": true,
  "bookingConfirmation": true,
  "bookingReminder": true,
  "checkInReminder": true,
  "checkOutReminder": true,
  "paymentNotifications": true,
  "promotionalOffers": false,
  "loyaltyUpdates": true,
  "reviewRequests": true,
  "accountAlerts": true,
  "quietHoursEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00"
}
```

### 12. Update User Preferences
```http
PUT /api/notifications/preferences/user/1
Content-Type: application/json

{
  "emailEnabled": true,
  "smsEnabled": false,
  "promotionalOffers": false,
  "quietHoursEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00"
}

Response: 200 OK
```

---

## 🗄️ Database Schema

### notifications Table
```sql
CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  recipient_email VARCHAR(255),
  recipient_phone VARCHAR(20),
  subject VARCHAR(200),
  message TEXT,
  template_id BIGINT,
  booking_id BIGINT,
  hotel_id BIGINT,
  status VARCHAR(20) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  retry_count INT NOT NULL DEFAULT 0,
  max_retries INT NOT NULL DEFAULT 3,
  error_message TEXT,
  scheduled_time TIMESTAMP,
  sent_time TIMESTAMP,
  read_time TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_notification_user (user_id),
  INDEX idx_notification_type (notification_type),
  INDEX idx_notification_is_read (is_read)
);
```

### notification_templates Table
```sql
CREATE TABLE notification_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_type VARCHAR(50) NOT NULL UNIQUE,
  template_name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  subject_template VARCHAR(200),
  message_template TEXT,
  html_template TEXT,
  channel VARCHAR(20) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  retry_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  max_retries INT NOT NULL DEFAULT 3,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_template_type (template_type)
);
```

### user_notification_preferences Table
```sql
CREATE TABLE user_notification_preferences (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  booking_confirmation BOOLEAN NOT NULL DEFAULT TRUE,
  booking_reminder BOOLEAN NOT NULL DEFAULT TRUE,
  check_in_reminder BOOLEAN NOT NULL DEFAULT TRUE,
  check_out_reminder BOOLEAN NOT NULL DEFAULT TRUE,
  payment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
  promotional_offers BOOLEAN NOT NULL DEFAULT TRUE,
  loyalty_updates BOOLEAN NOT NULL DEFAULT TRUE,
  review_requests BOOLEAN NOT NULL DEFAULT TRUE,
  account_alerts BOOLEAN NOT NULL DEFAULT TRUE,
  quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  quiet_hours_start VARCHAR(5),
  quiet_hours_end VARCHAR(5),
  unsubscribed_types VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_preference_user (user_id)
);
```

---

## ⚙️ Configuration

### Email Configuration (application.yaml)
```yaml
spring:
  mail:
    host: smtp.gmail.com              # SMTP server hostname
    port: 587                         # SMTP port (usually 587 for TLS)
    username: your-email@gmail.com    # Email account username
    password: app-password            # App password (not regular password)
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
    from: noreply@travolish.com      # From email address
    from-name: Travolish Hotels      # From name

# Environment variables (recommended for security)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@travolish.com
MAIL_FROM_NAME=Travolish Hotels
```

### SMS Configuration (application.yaml)
```yaml
sms:
  enabled: false                      # Enable SMS notifications
  provider: mock                      # Options: mock, twilio, aws-sns
  twilio:
    account-sid: AC...               # Twilio Account SID
    auth-token: auth_token           # Twilio Auth Token
    phone-number: +1234567890        # Twilio phone number

# Environment variables
SMS_ENABLED=true
SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890
```

### Scheduling Configuration
```yaml
spring:
  task:
    scheduling:
      pool:
        size: 2                       # Number of scheduler threads
      thread-name-prefix: notification-scheduler-
      shutdown:
        await-termination: true
        await-termination-period: 30s
```

---

## 🔄 Automatic Tasks

### 1. Process Scheduled Notifications
- **Frequency**: Every 60 seconds
- **Function**: Checks for notifications with SCHEDULED status and `scheduledTime <= now`
- **Action**: Sends the notification and updates status to SENT

### 2. Retry Failed Notifications
- **Frequency**: Every 5 minutes (300 seconds)
- **Function**: Finds FAILED notifications with retryCount < maxRetries
- **Action**: Attempts to resend and updates status/retry count

---

## 📧 Email Setup Guide

### Using Gmail
1. Enable 2-Factor Authentication on Gmail
2. Generate App Password (not regular password)
3. Use app password in configuration

### Using Other SMTP Providers
```
Outlook: smtp-mail.outlook.com:587
SendGrid: smtp.sendgrid.net:587
AWS SES: email-smtp.{region}.amazonaws.com:587
```

---

## 📱 SMS Setup Guide

### Using Twilio
1. Create Twilio account at twilio.com
2. Get Account SID and Auth Token
3. Purchase phone number for sending SMS
4. Configure in application.yaml
5. Install Twilio SDK (already in build.gradle)

### Integration Example
```java
// In SmsService, replace mock implementation with:
com.twilio.Twilio.init(accountSid, authToken);
Message message = Message.creator(
    new PhoneNumber("+1234567890"),  // To number
    new PhoneNumber(twilioPhoneNumber), // From number
    messageContent)
    .create();
```

---

## 🎯 Usage Examples

### Example 1: Send Booking Confirmation Email
```java
SendNotificationRequest request = new SendNotificationRequest();
request.setUserId(userId);
request.setType(NotificationType.BOOKING_CONFIRMATION);
request.setChannel(NotificationChannel.EMAIL);
request.setRecipientEmail("guest@example.com");
request.setSubject("Booking Confirmation - Order #12345");
request.setMessage("Dear Guest, your booking has been confirmed...");
request.setBookingId(bookingId);
request.setHotelId(hotelId);
request.setSendImmediately(true);

notificationService.sendNotification(request);
```

### Example 2: Schedule Check-in Reminder
```java
SendNotificationRequest request = new SendNotificationRequest();
request.setUserId(userId);
request.setType(NotificationType.CHECK_IN_REMINDER);
request.setChannel(NotificationChannel.EMAIL);
request.setRecipientEmail("guest@example.com");
request.setSubject("Check-in Reminder");
request.setMessage("Your check-in is tomorrow at 3 PM");
request.setScheduledTime(LocalDateTime.now().plusDays(1).withHour(10));
request.setSendImmediately(false);

notificationService.sendNotification(request);
```

### Example 3: Update User Preferences
```java
UserNotificationPreferenceDTO preferences = new UserNotificationPreferenceDTO();
preferences.setEmailEnabled(true);
preferences.setSmsEnabled(false);
preferences.setPromotionalOffers(false);
preferences.setQuietHoursEnabled(true);
preferences.setQuietHoursStart("22:00");
preferences.setQuietHoursEnd("08:00");

notificationService.updateUserPreferences(userId, preferences);
```

---

## 🛡️ Error Handling

### Notification Failures
- Failed notifications are automatically retried up to `maxRetries` times
- Retry interval: 5 minutes
- Error message is logged for debugging
- Status changes from FAILED to SENT on successful retry

### Common Errors
1. **Invalid Email Address** - Format validation before sending
2. **Invalid Phone Number** - Format validation for SMS
3. **SMTP Connection Failed** - Check email configuration
4. **SMS Service Disabled** - Enable SMS in config
5. **Missing Recipient** - Email or phone number must be provided

---

## 🔍 Monitoring & Analytics

### Query Examples

**Get unread count for user:**
```sql
SELECT COUNT(*) FROM notifications 
WHERE user_id = ? AND is_read = FALSE;
```

**Get failed notifications:**
```sql
SELECT * FROM notifications 
WHERE status = 'FAILED' AND retry_count < max_retries;
```

**Get notifications by type:**
```sql
SELECT * FROM notifications 
WHERE notification_type = ? 
ORDER BY created_at DESC;
```

---

## 🔐 Security Considerations

1. **Email Credentials**: Store in environment variables, not in code
2. **SMS API Keys**: Keep Twilio credentials secure
3. **User Preferences**: Respect user opt-out preferences
4. **Data Privacy**: Ensure GDPR compliance for EU users
5. **Rate Limiting**: Implement rate limiting for bulk notifications
6. **Audit Logging**: Log all notification activities

---

## 📈 Performance Optimization

1. **Database Indexes**: Optimized indexes on frequent queries
2. **Pagination**: All list endpoints are paginated
3. **Async Processing**: Email/SMS sent asynchronously
4. **Batch Operations**: Support for bulk notifications
5. **Connection Pooling**: SMTP connection caching
6. **Scheduling**: Efficient scheduled task processing

---

## 🚀 Deployment Checklist

- [ ] Configure email SMTP settings
- [ ] Test email sending
- [ ] Configure SMS service (if using)
- [ ] Set up notification templates
- [ ] Configure notification preferences for users
- [ ] Enable scheduled tasks
- [ ] Set up monitoring/logging
- [ ] Document custom templates
- [ ] Train support team on notification management
- [ ] Set up backup email service

---

## 📝 Future Enhancements

1. **Notification Center UI** - Web interface for managing notifications
2. **Push Notifications** - Firebase Cloud Messaging integration
3. **WhatsApp Notifications** - WhatsApp Business API integration
4. **Notification Analytics** - Delivery rates, open rates, click rates
5. **Template Builder** - WYSIWYG editor for templates
6. **A/B Testing** - Test different notification messages
7. **Advanced Scheduling** - Cron-based scheduling
8. **Notification Groups** - Group related notifications
9. **User Segments** - Target specific user groups
10. **Compliance** - GDPR, CCPA compliance features

---

## 📞 Support

For issues or questions:
1. Check logs: `docker logs traveller-service` (or your container)
2. Review email configuration
3. Verify database connectivity
4. Check notification templates exist
5. Review user preferences

---

**Version**: 1.0.0
**Last Updated**: December 2024
**Status**: Production Ready
