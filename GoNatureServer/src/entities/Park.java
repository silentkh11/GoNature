package entities;

import java.io.Serializable;

public class Park implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int parkId;
    private String name;
    private int maxCapacity;
    private int casualGap;
    private int estimatedStayTime;
    private int currentVisitors;

    public Park(int parkId, String name, int maxCapacity, int casualGap, int estimatedStayTime, int currentVisitors) {
        this.parkId = parkId;
        this.name = name;
        this.maxCapacity = maxCapacity;
        this.casualGap = casualGap;
        this.estimatedStayTime = estimatedStayTime;
        this.currentVisitors = currentVisitors;
    }

    // Getters
    public int getParkId() { return parkId; }
    public String getName() { return name; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getCasualGap() { return casualGap; }
    public int getEstimatedStayTime() { return estimatedStayTime; }
    public int getCurrentVisitors() { return currentVisitors; }
}