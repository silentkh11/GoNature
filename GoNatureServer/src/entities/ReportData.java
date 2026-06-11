package entities;

import java.io.Serializable;
import java.util.HashMap;

public class ReportData implements Serializable {
    private static final long serialVersionUID = 1L;

    private int parkId;
    private String month;
    private String year;
    
    // HashMaps are perfect here because they map a Category (e.g., "Family") to a Number (e.g., 150 visitors)
    private HashMap<String, Integer> visitorBreakdown;
    private HashMap<String, Double> incomeBreakdown;

    private int totalVisitors;
    private double totalIncome;

    public ReportData(int parkId, String month, String year) {
        this.parkId = parkId;
        this.month = month;
        this.year = year;
        this.visitorBreakdown = new HashMap<>();
        this.incomeBreakdown = new HashMap<>();
        this.totalVisitors = 0;
        this.totalIncome = 0.0;
    }

    public void addVisitorData(String type, int count) {
        visitorBreakdown.put(type, count);
        totalVisitors += count;
    }

    public void addIncomeData(String type, double income) {
        incomeBreakdown.put(type, income);
        totalIncome += income;
    }

    // Getters
    public int getParkId() { return parkId; }
    public String getMonth() { return month; }
    public String getYear() { return year; }
    public HashMap<String, Integer> getVisitorBreakdown() { return visitorBreakdown; }
    public HashMap<String, Double> getIncomeBreakdown() { return incomeBreakdown; }
    public int getTotalVisitors() { return totalVisitors; }
    public double getTotalIncome() { return totalIncome; }
}