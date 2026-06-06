package entities;

import java.io.Serializable;

public class VisitOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int orderId;
    private int parkId;
    private String visitorId;
    private String visitDate; 
    private String visitTime; 
    private int visitorCount;
    private String orderType;
    private String status;
    private double price;
    
    // --- NEW: Contact info for Guest Notifications ---
    private String email;
    private String phone;

    // Constructor 1: Used by Server & Guest Portal (Standard DB Fetch)
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

    // Constructor 2: Used by CreateOrderController (Includes Email/Phone for fresh bookings)
    public VisitOrder(int orderId, int parkId, String visitorId, String visitDate, String visitTime, int visitorCount, String orderType, String status, double price, String email, String phone) {
        this(orderId, parkId, visitorId, visitDate, visitTime, visitorCount, orderType, status, price); // Call base constructor
        this.email = email;
        this.phone = phone;
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
    public double getPrice() { return price; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    
    // --- Setters ---
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setStatus(String status) { this.status = status; }
    public void setPrice(double price) { this.price = price; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
}