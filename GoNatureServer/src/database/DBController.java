package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import entities.Employee;
import entities.Park;
import entities.VisitOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBController {

    // Singleton Instance
    private static DBController instance;
    
    // HikariCP Connection Pool
    private HikariDataSource dataSource;

    /**
     * Private constructor to initialize the database connection pool.
     */
    private DBController() {
        initPool();
    }

    /**
     * Retrieves the single active instance of the DBController.
     */
    public static synchronized DBController getInstance() {
        if (instance == null) {
            instance = new DBController();
        }
        return instance;
    }

    /**
     * Initializes the HikariCP connection pool with your exact MySQL configurations.
     */
    private void initPool() {
        HikariConfig config = new HikariConfig();
        
        // Your exact localized JDBC URL
        config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature_db?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
        
        // TODO: Ensure your local MySQL username and password match these!
        config.setUsername("root"); 
        config.setPassword("root"); // Change this to your actual MySQL root password
        
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);
        System.out.println("Database connection pool initialized successfully.");
    }

    /**
     * Grabs a connection from the pool. Use this in a try-with-resources block.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Closes the HikariCP connection pool cleanly.
     * Call this when the server is shutting down.
     */
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database connection pool closed successfully.");
        }
    }

    // =========================================================================
    // --- 1. LOGIN & AUTHENTICATION ---
    // =========================================================================

    /**
     * Verifies an employee's login credentials and returns their profile.
     */
    public static Employee verifyLogin(String username, String password) {
        String query = "SELECT * FROM employee WHERE username = ? AND password = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Employee(
                        rs.getInt("employee_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("park_id"),
                        rs.getString("username"),
                        rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error during Login:");
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // --- 2. PARK PARAMETER MANAGEMENT ---
    // =========================================================================

    /**
     * Fetches all details for a specific park.
     */
    public static Park getParkById(int parkId) {
        String query = "SELECT * FROM park WHERE park_id = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, parkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Park(
                        rs.getInt("park_id"),
                        rs.getString("name"),
                        rs.getInt("max_capacity"),
                        rs.getInt("casual_gap"),
                        rs.getInt("estimated_stay_time"),
                        rs.getInt("current_visitors")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates the three primary parameters of a park.
     */
    public static boolean updateParkParameters(Park updatedPark) {
        String query = "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, updatedPark.getMaxCapacity());
            stmt.setInt(2, updatedPark.getCasualGap());
            stmt.setInt(3, updatedPark.getEstimatedStayTime());
            stmt.setInt(4, updatedPark.getParkId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================================
    // --- 3. VISITOR BOOKING ENGINE ---
    // =========================================================================

    /**
     * Processes a new booking request. 
     * Automatically registers new visitors, validates capacity limits, and waitlists if full.
     */
    public static VisitOrder processNewOrder(VisitOrder newOrder) {
        try (Connection conn = getInstance().getConnection()) {
            
            // --- STEP 1: AUTO-REGISTER NEW VISITORS ---
            String checkVisitor = "SELECT visitor_id FROM visitor WHERE visitor_id = ?";
            try (PreparedStatement checkVisStmt = conn.prepareStatement(checkVisitor)) {
                checkVisStmt.setString(1, newOrder.getVisitorId());
                try (ResultSet rsVis = checkVisStmt.executeQuery()) {
                    
                    if (!rsVis.next()) { 
                        System.out.println("> New visitor detected. Auto-registering ID: " + newOrder.getVisitorId());
                        String insertVisitor = "INSERT INTO visitor (visitor_id, email, visitor_type) VALUES (?, ?, 'Regular')";
                        try (PreparedStatement insertVisStmt = conn.prepareStatement(insertVisitor)) {
                            insertVisStmt.setString(1, newOrder.getVisitorId());
                            insertVisStmt.setString(2, "guest_" + newOrder.getVisitorId() + "@gonature.co.il");
                            insertVisStmt.executeUpdate();
                        }
                    }
                }
            }

            // --- STEP 2: GET PARK CAPACITY LIMITS ---
            Park park = getParkById(newOrder.getParkId());
            if (park == null) return null; 
            
            int onlineBookingLimit = park.getMaxCapacity() - park.getCasualGap();
            
            // --- STEP 3: CALCULATE CURRENTLY BOOKED VISITORS ---
            String checkCapacityQuery = "SELECT SUM(visitor_count) as total_booked FROM visit_order " +
                                        "WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status != 'Cancelled'";
            
            int currentlyBooked = 0;
            try (PreparedStatement checkCapStmt = conn.prepareStatement(checkCapacityQuery)) {
                checkCapStmt.setInt(1, newOrder.getParkId());
                checkCapStmt.setString(2, newOrder.getVisitDate());
                checkCapStmt.setString(3, newOrder.getVisitTime());
                
                try (ResultSet rs = checkCapStmt.executeQuery()) {
                    if (rs.next()) {
                        currentlyBooked = rs.getInt("total_booked");
                    }
                }
            }
            
            // --- STEP 4: ALGORITHM DECISION ---
            if (currentlyBooked + newOrder.getVisitorCount() <= onlineBookingLimit) {
                newOrder.setStatus("Confirmed");
            } else {
                newOrder.setStatus("Waitlisted");
            }
            
            // --- STEP 5: INSERT THE FINAL ORDER ---
            String insertQuery = "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, visitor_count, order_type, status) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?)";
                                 
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, newOrder.getParkId());
                insertStmt.setString(2, newOrder.getVisitorId());
                insertStmt.setString(3, newOrder.getVisitDate());
                insertStmt.setString(4, newOrder.getVisitTime());
                insertStmt.setInt(5, newOrder.getVisitorCount());
                insertStmt.setString(6, newOrder.getOrderType());
                insertStmt.setString(7, newOrder.getStatus());
                
                insertStmt.executeUpdate();
                
                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newOrder.setOrderId(generatedKeys.getInt(1));
                    }
                }
                return newOrder; 
            }
            
        } catch (SQLException e) {
            System.err.println("Database error during order processing:");
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // --- 4. PARK ENTRANCE GATE LOGIC ---
    // =========================================================================

    /**
     * Processes a park entry request at the gate.
     * Validates the order, updates park capacity, and changes order status.
     */
    public static String processParkEntry(int orderId) {
        try (Connection conn = getInstance().getConnection()) {
            
            // --- STEP 1: VERIFY THE ORDER ---
            String checkOrderQuery = "SELECT park_id, visitor_count, status FROM visit_order WHERE order_id = ?";
            int parkId = 0;
            int visitorCount = 0;
            String status = "";
            
            try (PreparedStatement checkStmt = conn.prepareStatement(checkOrderQuery)) {
                checkStmt.setInt(1, orderId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        parkId = rs.getInt("park_id");
                        visitorCount = rs.getInt("visitor_count");
                        status = rs.getString("status");
                    } else {
                        return "Error: Order #" + orderId + " does not exist.";
                    }
                }
            }
            
            // --- STEP 2: BUSINESS LOGIC VALIDATION ---
            if (status.equals("In Park")) {
                return "Error: This order has already been scanned and admitted.";
            } else if (status.equals("Waitlisted") || status.equals("Cancelled")) {
                return "Error: Cannot admit. Order status is: " + status;
            } else if (!status.equals("Confirmed")) {
                return "Error: Unrecognized order status.";
            }
            
            // --- STEP 3: UPDATE DATABASE (TRANSACTION) ---
            String updateOrder = "UPDATE visit_order SET status = 'In Park' WHERE order_id = ?";
            try (PreparedStatement updateOrderStmt = conn.prepareStatement(updateOrder)) {
                updateOrderStmt.setInt(1, orderId);
                updateOrderStmt.executeUpdate();
            }
            
            String updatePark = "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";
            try (PreparedStatement updateParkStmt = conn.prepareStatement(updatePark)) {
                updateParkStmt.setInt(1, visitorCount);
                updateParkStmt.setInt(2, parkId);
                updateParkStmt.executeUpdate();
            }
            
            return "SUCCESS: Admitted " + visitorCount + " visitors! (Order #" + orderId + ")";
            
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during entry processing.";
        }
    }
    
 // =========================================================================
    // --- 5. DEPARTMENT MANAGER APPROVAL WORKFLOW ---
    // =========================================================================

    /**
     * Replaces the old direct update. Now, the Park Manager submits a request to the holding pen.
     */
    public static boolean submitParameterRequest(entities.Park requestedUpdate) {
        String query = "INSERT INTO parameter_request (park_id, new_max_capacity, new_casual_gap, new_estimated_stay_time, status) VALUES (?, ?, ?, ?, 'Pending')";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, requestedUpdate.getParkId());
            stmt.setInt(2, requestedUpdate.getMaxCapacity());
            stmt.setInt(3, requestedUpdate.getCasualGap());
            stmt.setInt(4, requestedUpdate.getEstimatedStayTime());
            
            return stmt.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all "Pending" requests for the Department Manager to review.
     * Uses a JOIN to grab the actual Park Name so the manager knows what they are looking at.
     */
    public static java.util.ArrayList<entities.ParameterRequest> getPendingRequests() {
        java.util.ArrayList<entities.ParameterRequest> requests = new java.util.ArrayList<>();
        String query = "SELECT pr.*, p.name as park_name FROM parameter_request pr JOIN park p ON pr.park_id = p.park_id WHERE pr.status = 'Pending'";
        
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query);
             java.sql.ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                requests.add(new entities.ParameterRequest(
                    rs.getInt("request_id"),
                    rs.getInt("park_id"),
                    rs.getString("park_name"),
                    rs.getInt("new_max_capacity"),
                    rs.getInt("new_casual_gap"),
                    rs.getInt("new_estimated_stay_time"),
                    rs.getString("status")
                ));
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Handles the Department Manager's decision.
     * If approved, it updates the actual park table AND marks the request as Approved.
     */
    public static boolean processParameterDecision(int requestId, String decision) {
        try (java.sql.Connection conn = getInstance().getConnection()) {
            
            if (decision.equals("Approved")) {
                // 1. Fetch the request details
                String fetchReq = "SELECT * FROM parameter_request WHERE request_id = ?";
                try (java.sql.PreparedStatement fetchStmt = conn.prepareStatement(fetchReq)) {
                    fetchStmt.setInt(1, requestId);
                    try (java.sql.ResultSet rs = fetchStmt.executeQuery()) {
                        if (rs.next()) {
                            // 2. Update the actual park parameters
                            String updatePark = "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
                            try (java.sql.PreparedStatement parkStmt = conn.prepareStatement(updatePark)) {
                                parkStmt.setInt(1, rs.getInt("new_max_capacity"));
                                parkStmt.setInt(2, rs.getInt("new_casual_gap"));
                                parkStmt.setInt(3, rs.getInt("new_estimated_stay_time"));
                                parkStmt.setInt(4, rs.getInt("park_id"));
                                parkStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
            
            // 3. Mark the request as Approved or Denied
            String updateReq = "UPDATE parameter_request SET status = ? WHERE request_id = ?";
            try (java.sql.PreparedStatement reqStmt = conn.prepareStatement(updateReq)) {
                reqStmt.setString(1, decision);
                reqStmt.setInt(2, requestId);
                return reqStmt.executeUpdate() > 0;
            }
            
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}