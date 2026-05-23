package database;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBController {
    
    private static DBController instance;
    private HikariDataSource dataSource;

    private DBController() {}

    public static DBController getInstance() {
        if (instance == null) {
            instance = new DBController();
        }
        return instance;
    }

    // We keep this static method EXACTLY as your ServerPortFrameController expects it!
    public static void connectToDB() throws Exception {
        getInstance().initPool();
    }

    private void initPool() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // Already connected
        }
        
        HikariConfig config = new HikariConfig();
        // Pointing to your new phase 2 database schema
        
        config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature_db?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false");
        config.setUsername("root");
        config.setPassword("root"); // Change this to your actual MySQL password!
        
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool is not initialized.");
        }
        return dataSource.getConnection();
    }
    
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    /**
     * Checks the database for a matching username and password.
     * @return An Employee object if successful, or null if login fails.
     */
    public static entities.Employee verifyLogin(String username, String password) {
        String query = "SELECT * FROM employee WHERE username = ? AND password = ?";
        
        // Safely borrow a connection from the HikariCP pool
        try (Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Match found! Build the Employee object.
                    int empId = rs.getInt("employee_id");
                    String fName = rs.getString("first_name");
                    String lName = rs.getString("last_name");
                    String email = rs.getString("email");
                    String role = rs.getString("role");
                    
                    // Handle park_id carefully since Dept Managers might have a NULL park_id
                    Integer parkId = rs.getInt("park_id");
                    if (rs.wasNull()) {
                        parkId = null;
                    }
                    
                    return new entities.Employee(empId, fName, lName, email, role, parkId, username);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL ERROR during login verification:");
            e.printStackTrace();
        }
        
        // Return null if no match was found or an error occurred
        return null;
    }
    
    /**
     * Fetches all details for a specific park.
     */
    public static entities.Park getParkById(int parkId) {
        String query = "SELECT * FROM park WHERE park_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, parkId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new entities.Park(
                        rs.getInt("park_id"),
                        rs.getString("name"),
                        rs.getInt("max_capacity"),
                        rs.getInt("casual_gap"),
                        rs.getInt("estimated_stay_time"),
                        rs.getInt("current_visitors")
                    );
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates the three primary parameters of a park.
     */
    public static boolean updateParkParameters(entities.Park updatedPark) {
        String query = "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, updatedPark.getMaxCapacity());
            stmt.setInt(2, updatedPark.getCasualGap());
            stmt.setInt(3, updatedPark.getEstimatedStayTime());
            stmt.setInt(4, updatedPark.getParkId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Processes a new booking request. 
     * Validates capacity limits and automatically waitlists if full.
     */
    public static entities.VisitOrder processNewOrder(entities.VisitOrder newOrder) {
        // Step 1: Get the park's absolute limits
        entities.Park park = getParkById(newOrder.getParkId());
        if (park == null) return null; // Park doesn't exist
        
        int onlineBookingLimit = park.getMaxCapacity() - park.getCasualGap();
        
        // Step 2: Sum the visitors already booked for this specific date and time
        String checkCapacityQuery = "SELECT SUM(visitor_count) as total_booked FROM visit_order " +
                                    "WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status != 'Cancelled'";
        
        int currentlyBooked = 0;
        
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkCapacityQuery)) {
             
            checkStmt.setInt(1, newOrder.getParkId());
            checkStmt.setString(2, newOrder.getVisitDate());
            checkStmt.setString(3, newOrder.getVisitTime());
            
            try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    currentlyBooked = rs.getInt("total_booked");
                }
            }
            
            // Step 3: Algorithm Decision (Confirm or Waitlist)
            if (currentlyBooked + newOrder.getVisitorCount() <= onlineBookingLimit) {
                newOrder.setStatus("Confirmed");
            } else {
                newOrder.setStatus("Waitlisted");
            }
            
            // Step 4: Insert into the Database
            String insertQuery = "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, visitor_count, order_type, status) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?)";
                                 
            // Return generated keys allows us to grab the auto-incremented order_id!
            try (java.sql.PreparedStatement insertStmt = conn.prepareStatement(insertQuery, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, newOrder.getParkId());
                insertStmt.setString(2, newOrder.getVisitorId());
                insertStmt.setString(3, newOrder.getVisitDate());
                insertStmt.setString(4, newOrder.getVisitTime());
                insertStmt.setInt(5, newOrder.getVisitorCount());
                insertStmt.setString(6, newOrder.getOrderType());
                insertStmt.setString(7, newOrder.getStatus());
                
                insertStmt.executeUpdate();
                
                // Grab the official order ID created by MySQL and attach it to the object
                try (java.sql.ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newOrder.setOrderId(generatedKeys.getInt(1));
                    }
                }
                
                return newOrder; // Return the fully finalized order back to the client
            }
            
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}