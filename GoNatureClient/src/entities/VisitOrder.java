package entities;

import java.io.Serializable;

public class VisitOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int orderId;
    private int parkId;
    private String visitorId;
    private String visitDate; // Format: "YYYY-MM-DD"
    private String visitTime; // Format: "HH:mm"
    private int visitorCount;
    private String orderType;
    private String status;

    public VisitOrder(int orderId, int parkId, String visitorId, String visitDate, String visitTime, int visitorCount, String orderType, String status) {
        this.orderId = orderId;
        this.parkId = parkId;
        this.visitorId = visitorId;
        this.visitDate = visitDate;
        this.visitTime = visitTime;
        this.visitorCount = visitorCount;
        this.orderType = orderType;
        this.status = status;
    }

    // --- Getters ---
    public int getOrderId() { return orderId; }
    public int getParkId() { return parkId; }
    public String getVisitorId() { return visitorId; }
    public String getVisitDate() { return visitDate; }
    public String getVisitTime() { return visitTime; }
    public int getVisitorCount() { return visitorCount; }
    public String getOrderType() { return orderType; }
    public String getStatus() { return status; }
    
    // --- Setters (Needed for the Server to update the status and ID) ---
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setStatus(String status) { this.status = status; }
}