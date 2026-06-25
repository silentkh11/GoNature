package entities;

import java.io.Serializable;
import java.sql.Date; // Use java.sql.Date for database dates

/**
 * Legacy order entity retained for compatibility.
 * New bookings use {@link VisitOrder} instead.
 */
public class Order implements Serializable {
    
    // This ID is required by Serializable to ensure both sides have the same version of the class
    private static final long serialVersionUID = 1L; 

    private int orderNumber;
    private Date orderDate;
    private int numberOfVisitors;
    private int confirmationCode;
    private int subscriberId;
    private Date dateOfPlacingOrder;

    // Constructor
    public Order(int orderNumber, Date orderDate, int numberOfVisitors, int confirmationCode, int subscriberId, Date dateOfPlacingOrder) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.numberOfVisitors = numberOfVisitors;
        this.confirmationCode = confirmationCode;
        this.subscriberId = subscriberId;
        this.dateOfPlacingOrder = dateOfPlacingOrder;
    }

    // Getters and Setters
    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public int getNumberOfVisitors() { return numberOfVisitors; }
    public void setNumberOfVisitors(int numberOfVisitors) { this.numberOfVisitors = numberOfVisitors; }

    public int getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(int confirmationCode) { this.confirmationCode = confirmationCode; }

    public int getSubscriberId() { return subscriberId; }
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }

    public Date getDateOfPlacingOrder() { return dateOfPlacingOrder; }
    public void setDateOfPlacingOrder(Date dateOfPlacingOrder) { this.dateOfPlacingOrder = dateOfPlacingOrder; }

    // A nice toString method makes debugging much easier!
    @Override
    public String toString() {
        return "Order [orderNumber=" + orderNumber + ", visitors=" + numberOfVisitors + ", date=" + orderDate + "]";
    }
}