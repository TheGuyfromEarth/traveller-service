package com.travolish.traveller.payment.entity;

public enum ReceiptStatus {
    DRAFT,          // Receipt created but not finalized
    GENERATED,      // Receipt PDF generated
    SENT,           // Receipt sent to user email
    DOWNLOADED      // User downloaded receipt
}
