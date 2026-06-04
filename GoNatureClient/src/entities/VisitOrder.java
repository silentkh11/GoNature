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
    private double price; // <-- NEW: Holds the final calculated GoNature price

    // Updated Constructor
    public VisitOrder(int orderId, int parkId, String visitorId, String visitDate, String visitTime, int visitorCount, String orderType, String status, double price) {
        this.orderId = orderId;
        this.parkId = parkId;
        this.visitorId = visitorId;
        this.visitDate = visitDate;
        this.visitTime = visitTime;
        this.visitorCount = visitorCount;
        this.orderType = orderType;
        this.status = status;
        this.price = price;
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
    public double getPrice() { return price; } // <-- New Getter
    
    // --- Setters ---
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setStatus(String status) { this.status = status; }
    public void setPrice(double price) { this.price = price; } // <-- New Setter
}