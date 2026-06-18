package entities;

import java.io.Serializable;

/**
 * Represents a promotional discount request submitted by a Park Manager.
 * Requires Department Manager approval before it becomes active.
 */
public class Promotion implements Serializable {
    private static final long serialVersionUID = 1L;

    private int promotionId;
    private int parkId;
    private String parkName;
    private double discountPercent;
    private String status;

    public Promotion(int promotionId, int parkId, String parkName, double discountPercent, String status) {
        this.promotionId = promotionId;
        this.parkId = parkId;
        this.parkName = parkName;
        this.discountPercent = discountPercent;
        this.status = status;
    }

    public int getPromotionId()     { return promotionId; }
    public int getParkId()          { return parkId; }
    public String getParkName()     { return parkName; }
    public double getDiscountPercent() { return discountPercent; }
    public String getStatus()       { return status; }
}
