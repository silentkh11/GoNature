package entities;

import java.io.Serializable;

/**
 * Represents a nature park managed by the GoNature system.
 * Includes capacity parameters and the currently active promotional discount.
 */
public class Park implements Serializable {
    private static final long serialVersionUID = 1L;

    private int parkId;
    private String name;
    private int maxCapacity;
    private int casualGap;
    private int estimatedStayTime;
    private int currentVisitors;
    private double activeDiscount;

    /** Full constructor including active promotional discount. */
    public Park(int parkId, String name, int maxCapacity, int casualGap,
                int estimatedStayTime, int currentVisitors, double activeDiscount) {
        this.parkId = parkId;
        this.name = name;
        this.maxCapacity = maxCapacity;
        this.casualGap = casualGap;
        this.estimatedStayTime = estimatedStayTime;
        this.currentVisitors = currentVisitors;
        this.activeDiscount = activeDiscount;
    }

    /** Backward-compatible constructor (activeDiscount defaults to 0). */
    public Park(int parkId, String name, int maxCapacity, int casualGap,
                int estimatedStayTime, int currentVisitors) {
        this(parkId, name, maxCapacity, casualGap, estimatedStayTime, currentVisitors, 0.0);
    }

    public int getParkId()            { return parkId; }
    public String getName()           { return name; }
    public int getMaxCapacity()       { return maxCapacity; }
    public int getCasualGap()         { return casualGap; }
    public int getEstimatedStayTime() { return estimatedStayTime; }
    public int getCurrentVisitors()   { return currentVisitors; }
    public double getActiveDiscount() { return activeDiscount; }
}
