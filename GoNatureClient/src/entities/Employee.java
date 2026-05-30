package entities;

import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Integer parkId; // Must be an Integer object so it can hold 'null' for Dept Managers!

    // The precise 6-parameter constructor the DBController is looking for
    public Employee(int employeeId, String firstName, String lastName, String email, String role, Integer parkId) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.parkId = parkId;
    }

    // --- Getters ---
    public int getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Integer getParkId() { return parkId; }
    
    // --- Setters (Optional but good practice) ---
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setParkId(Integer parkId) { this.parkId = parkId; }
}