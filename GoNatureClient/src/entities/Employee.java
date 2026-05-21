package entities;

import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Integer parkId; // Integer instead of int so it can be null for Dept Managers
    private String username;

    public Employee(int employeeId, String firstName, String lastName, String email, String role, Integer parkId, String username) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.parkId = parkId;
        this.username = username;
    }

    // --- Getters ---
    public int getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Integer getParkId() { return parkId; }
    public String getUsername() { return username; }
}