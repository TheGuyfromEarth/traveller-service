package com.travolish.traveller.payout;

public final class HostEarningStatus {

    private HostEarningStatus() {}

    /** Paid by guest; held until check-out passes. */
    public static final String PENDING_RELEASE = "PENDING_RELEASE";
    /** Available for host payout request. */
    public static final String EARNED = "EARNED";
    /** Linked to an in-flight payout. */
    public static final String PAYOUT_PENDING = "PAYOUT_PENDING";
    public static final String PAID = "PAID";
    public static final String REFUNDED = "REFUNDED";
}
