package com.travolish.traveller.analytics.dto;

public class ReturningGuestDTO {
    private Long guestId;
    private String guestName;
    private String guestEmail;
    private Integer visitCount;
    private Double totalSpent;
    private String lastVisitDate;
    private String favoriteRoom;

    public ReturningGuestDTO() {}

    public ReturningGuestDTO(Long guestId, String guestName, String guestEmail, Integer visitCount,
                            Double totalSpent, String lastVisitDate, String favoriteRoom) {
        this.guestId = guestId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.visitCount = visitCount;
        this.totalSpent = totalSpent;
        this.lastVisitDate = lastVisitDate;
        this.favoriteRoom = favoriteRoom;
    }

    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public String getLastVisitDate() { return lastVisitDate; }
    public void setLastVisitDate(String lastVisitDate) { this.lastVisitDate = lastVisitDate; }

    public String getFavoriteRoom() { return favoriteRoom; }
    public void setFavoriteRoom(String favoriteRoom) { this.favoriteRoom = favoriteRoom; }
}
