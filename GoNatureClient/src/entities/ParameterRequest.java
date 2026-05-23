package entities;

import java.io.Serializable;

public class ParameterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int requestId;
    private int parkId;
    private String parkName; // Useful for the Dept Manager to see which park is requesting it
    private int newMaxCapacity;
    private int newCasualGap;
    private int newEstimatedStayTime;
    private String status;

    public ParameterRequest(int requestId, int parkId, String parkName, int newMaxCapacity, int newCasualGap, int newEstimatedStayTime, String status) {
        this.requestId = requestId;
        this.parkId = parkId;
        this.parkName = parkName;
        this.newMaxCapacity = newMaxCapacity;
        this.newCasualGap = newCasualGap;
        this.newEstimatedStayTime = newEstimatedStayTime;
        this.status = status;
    }

    // Getters
    public int getRequestId() { return requestId; }
    public int getParkId() { return parkId; }
    public String getParkName() { return parkName; }
    public int getNewMaxCapacity() { return newMaxCapacity; }
    public int getNewCasualGap() { return newCasualGap; }
    public int getNewEstimatedStayTime() { return newEstimatedStayTime; }
    public String getStatus() { return status; }
}