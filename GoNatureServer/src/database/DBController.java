package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBController {

    private static DBController instance;
    private HikariDataSource dataSource;

    private DBController() {
        initPool();
    }

    public static synchronized DBController getInstance() {
        if (instance == null) {
            instance = new DBController();
        }
        return instance;
    }

    private void initPool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature_db?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword("123456"); // UPDATE THIS IF NECESSARY
            
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized successfully.");
        } catch (Exception e) {
            System.err.println("Failed to initialize database pool: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // =========================================================================
    // --- 1. LOGIN ---
    // =========================================================================
    public static entities.Employee verifyLogin(String username, String password) {
        String query = "SELECT * FROM employee WHERE username = ? AND password = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new entities.Employee(
                        rs.getInt("employee_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("park_id") == 0 ? null : rs.getInt("park_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // --- 2. BOOKING & PRICING ENGINE ---
    // =========================================================================
    public static boolean hasDuplicateBooking(entities.VisitOrder order) {
        String query = "SELECT order_id FROM visit_order WHERE visitor_id = ? AND park_id = ? AND visit_date = ? AND status IN ('Confirmed', 'Waitlisted', 'In Park')";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, order.getVisitorId());
            stmt.setInt(2, order.getParkId());
            stmt.setString(3, order.getVisitDate());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); 
            }
        } catch (SQLException e) {
            System.err.println("Database error during duplicate check: " + e.getMessage());
        }
        return false;
    }

    public static entities.VisitOrder processNewOrder(entities.VisitOrder order) {
        
        String ensureVisitorQuery = "INSERT IGNORE INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) VALUES (?, 'Guest', 'Visitor', 'Not Provided', 'Not Provided', 'Regular')";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement visitorStmt = conn.prepareStatement(ensureVisitorQuery)) {
            visitorStmt.setString(1, order.getVisitorId());
            visitorStmt.executeUpdate(); 
        } catch (SQLException e) {
            System.err.println("Database Error: Could not create temporary visitor profile - " + e.getMessage());
            return null; 
        }

        try (Connection conn = getInstance().getConnection()) {
            
            int maxCapacity = 0, casualGap = 0, currentBooked = 0;
            String parkQuery = "SELECT max_capacity, casual_gap FROM park WHERE park_id = ?";
            try (PreparedStatement parkStmt = conn.prepareStatement(parkQuery)) {
                parkStmt.setInt(1, order.getParkId());
                try (ResultSet rs = parkStmt.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity = rs.getInt("max_capacity");
                        casualGap = rs.getInt("casual_gap");
                    }
                }
            }

            String sumQuery = "SELECT SUM(visitor_count) AS total_visitors FROM visit_order WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status IN ('Confirmed', 'In Park')";
            try (PreparedStatement sumStmt = conn.prepareStatement(sumQuery)) {
                sumStmt.setInt(1, order.getParkId());
                sumStmt.setString(2, order.getVisitDate());
                sumStmt.setString(3, order.getVisitTime());
                try (ResultSet rs = sumStmt.executeQuery()) {
                    if (rs.next()) {
                        currentBooked = rs.getInt("total_visitors");
                    }
                }
            }

            int availableForPreorder = maxCapacity - casualGap;
            String assignedStatus = ((currentBooked + order.getVisitorCount()) > availableForPreorder) ? "Waitlisted" : "Confirmed";
            
            double calculatedPrice = 0.0;
            int basePrice = 100;
            boolean isSubscriber = false;
            boolean isGuide = false;

            String subQuery = "SELECT is_guide FROM subscriber WHERE visitor_id = ?";
            try (PreparedStatement subStmt = conn.prepareStatement(subQuery)) {
                subStmt.setString(1, order.getVisitorId());
                try (ResultSet subRs = subStmt.executeQuery()) {
                    if (subRs.next()) {
                        isSubscriber = true;
                        isGuide = subRs.getBoolean("is_guide");
                    }
                }
            }

            if (order.getOrderType().equalsIgnoreCase("Group") || isGuide) {
                int payingVisitors = Math.max(0, order.getVisitorCount() - 1); 
                calculatedPrice = payingVisitors * (basePrice * 0.75); 
            } else if (isSubscriber) {
                calculatedPrice = order.getVisitorCount() * (basePrice * 0.80);
            } else {
                calculatedPrice = order.getVisitorCount() * (basePrice * 0.85);
            }
            
            order.setPrice(calculatedPrice); 

            String insertQuery = "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, visitor_count, order_type, status, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, order.getParkId());
                insertStmt.setString(2, order.getVisitorId());
                insertStmt.setString(3, order.getVisitDate());
                insertStmt.setString(4, order.getVisitTime());
                insertStmt.setInt(5, order.getVisitorCount());
                insertStmt.setString(6, order.getOrderType());
                insertStmt.setString(7, assignedStatus); 
                insertStmt.setDouble(8, calculatedPrice); 
                
                insertStmt.executeUpdate();
                
                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        order.setOrderId(generatedKeys.getInt(1));
                        order.setStatus(assignedStatus);
                        return order;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // --- 3. GATE ENTRY & EXIT (REAL-TIME CAPACITY TRACKING) ---
    // =========================================================================
    public static String processParkEntry(int orderId) {
        String selectQuery = "SELECT status, park_id, visitor_count FROM visit_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE visit_order SET status = 'In Park' WHERE order_id = ?";
        String updateParkQuery = "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";
        
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false); // Start strict transaction
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, orderId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        int parkId = rs.getInt("park_id");
                        int visitors = rs.getInt("visitor_count");
                        
                        if (status.equals("Confirmed")) {
                            
                            // 1. Mark ticket as Used
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateOrderQuery)) {
                                updateStmt.setInt(1, orderId);
                                updateStmt.executeUpdate();
                            }
                            
                            // 2. Add visitors to the Park's physical count
                            try (PreparedStatement parkStmt = conn.prepareStatement(updateParkQuery)) {
                                parkStmt.setInt(1, visitors);
                                parkStmt.setInt(2, parkId);
                                parkStmt.executeUpdate();
                            }
                            
                            conn.commit(); // Save both changes permanently
                            return "SUCCESS: " + visitors + " visitors admitted to the park.";
                            
                        } else {
                            conn.rollback();
                            return "Order status is '" + status + "', not Confirmed.";
                        }
                    } else {
                        conn.rollback();
                        return "Order ID not found.";
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database Error.";
        }
    }

    public static String processParkExit(int orderId) {
        String selectQuery = "SELECT status, park_id, visitor_count FROM visit_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE visit_order SET status = 'Completed' WHERE order_id = ?";
        String updateParkQuery = "UPDATE park SET current_visitors = current_visitors - ? WHERE park_id = ?";
        
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false); // Start strict transaction
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, orderId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        int parkId = rs.getInt("park_id");
                        int visitors = rs.getInt("visitor_count");
                        
                        if (status.equals("In Park")) {
                            
                            // 1. Mark ticket as Completed
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateOrderQuery)) {
                                updateStmt.setInt(1, orderId);
                                updateStmt.executeUpdate();
                            }
                            
                            // 2. Subtract visitors to free up Park capacity!
                            try (PreparedStatement parkStmt = conn.prepareStatement(updateParkQuery)) {
                                parkStmt.setInt(1, visitors);
                                parkStmt.setInt(2, parkId);
                                parkStmt.executeUpdate();
                            }
                            
                            conn.commit(); // Save both changes permanently
                            return "SUCCESS: " + visitors + " visitors have safely exited the park.";
                            
                        } else {
                            conn.rollback();
                            return "Order status is '" + status + "'. Visitor must be 'In Park' to exit.";
                        }
                    } else {
                        conn.rollback();
                        return "Order ID not found.";
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database Error.";
        }
    }

    // =========================================================================
    // --- 4. PARK MANAGER ---
    // =========================================================================
    public static entities.Park getParkById(int parkId) {
        String query = "SELECT * FROM park WHERE park_id = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, parkId);
            try (ResultSet rs = stmt.executeQuery()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean submitParameterRequest(entities.Park park) {
        String query = "INSERT INTO parameter_request (park_id, new_max_capacity, new_casual_gap, new_estimated_stay_time, status) VALUES (?, ?, ?, ?, 'Pending')";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, park.getParkId());
            stmt.setInt(2, park.getMaxCapacity());
            stmt.setInt(3, park.getCasualGap());
            stmt.setInt(4, park.getEstimatedStayTime());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // --- 5. DEPARTMENT MANAGER ---
    // =========================================================================
    public static ArrayList<entities.ParameterRequest> getPendingRequests() {
        ArrayList<entities.ParameterRequest> requests = new ArrayList<>();
        String query = "SELECT pr.*, p.name FROM parameter_request pr JOIN park p ON pr.park_id = p.park_id WHERE pr.status = 'Pending'";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                requests.add(new entities.ParameterRequest(
                    rs.getInt("request_id"),
                    rs.getInt("park_id"),
                    rs.getString("name"),
                    rs.getInt("new_max_capacity"),
                    rs.getInt("new_casual_gap"),
                    rs.getInt("new_estimated_stay_time"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public static boolean processParameterDecision(int requestId, String decision) {
        String updateReqQuery = "UPDATE parameter_request SET status = ? WHERE request_id = ?";
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement reqStmt = conn.prepareStatement(updateReqQuery)) {
                reqStmt.setString(1, decision);
                reqStmt.setInt(2, requestId);
                reqStmt.executeUpdate();
            }

            if (decision.equals("Approved")) {
                String fetchQuery = "SELECT * FROM parameter_request WHERE request_id = ?";
                int parkId, newMax, newGap, newStay;
                try (PreparedStatement fetchStmt = conn.prepareStatement(fetchQuery)) {
                    fetchStmt.setInt(1, requestId);
                    try (ResultSet rs = fetchStmt.executeQuery()) {
                        if (rs.next()) {
                            parkId = rs.getInt("park_id");
                            newMax = rs.getInt("new_max_capacity");
                            newGap = rs.getInt("new_casual_gap");
                            newStay = rs.getInt("new_estimated_stay_time");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                
                String updateParkQuery = "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
                try (PreparedStatement parkStmt = conn.prepareStatement(updateParkQuery)) {
                    parkStmt.setInt(1, newMax);
                    parkStmt.setInt(2, newGap);
                    parkStmt.setInt(3, newStay);
                    parkStmt.setInt(4, parkId);
                    parkStmt.executeUpdate();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================================
    // --- 6. SERVICE REPRESENTATIVE ---
    // =========================================================================
    public static String registerNewSubscriber(entities.Subscriber sub) {
        try (Connection conn = getInstance().getConnection()) {
            String checkSubQuery = "SELECT subscriber_id FROM subscriber WHERE visitor_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSubQuery)) {
                checkStmt.setString(1, sub.getVisitorId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) return "Error: ID " + sub.getVisitorId() + " is already a registered subscriber.";
                }
            }

            String visType = sub.isGuide() ? "Guide" : "Subscriber";
            String checkVisQuery = "SELECT visitor_id FROM visitor WHERE visitor_id = ?";
            boolean visitorExists = false;
            
            try (PreparedStatement checkVisStmt = conn.prepareStatement(checkVisQuery)) {
                checkVisStmt.setString(1, sub.getVisitorId());
                try (ResultSet rs = checkVisStmt.executeQuery()) {
                    if (rs.next()) visitorExists = true;
                }
            }

            if (visitorExists) {
                String updateVis = "UPDATE visitor SET first_name=?, last_name=?, email=?, phone=?, visitor_type=? WHERE visitor_id=?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateVis)) {
                    updateStmt.setString(1, sub.getFirstName());
                    updateStmt.setString(2, sub.getLastName());
                    updateStmt.setString(3, sub.getEmail());
                    updateStmt.setString(4, sub.getPhone());
                    updateStmt.setString(5, visType);
                    updateStmt.setString(6, sub.getVisitorId());
                    updateStmt.executeUpdate();
                }
            } else {
                String insertVis = "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertVis)) {
                    insertStmt.setString(1, sub.getVisitorId());
                    insertStmt.setString(2, sub.getFirstName());
                    insertStmt.setString(3, sub.getLastName());
                    insertStmt.setString(4, sub.getEmail());
                    insertStmt.setString(5, sub.getPhone());
                    insertStmt.setString(6, visType);
                    insertStmt.executeUpdate();
                }
            }

            String insertSubQuery = "INSERT INTO subscriber (visitor_id, family_size, credit_card, is_guide) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertSubStmt = conn.prepareStatement(insertSubQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertSubStmt.setString(1, sub.getVisitorId());
                insertSubStmt.setInt(2, sub.getFamilySize());
                insertSubStmt.setString(3, sub.getCreditCard());
                insertSubStmt.setBoolean(4, sub.isGuide());
                insertSubStmt.executeUpdate();

                try (ResultSet generatedKeys = insertSubStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newSubId = generatedKeys.getInt(1);
                        return "SUCCESS: " + visType + " registered! (Sub ID: #" + newSubId + ")";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during registration.";
        }
        return "Error: Registration failed due to an unknown database issue.";
    }

    // =========================================================================
    // --- 7. GUEST PORTAL & WAITLIST PROMOTION ENGINE ---
    // =========================================================================
    public static ArrayList<entities.VisitOrder> getGuestOrders(String visitorId) {
        ArrayList<entities.VisitOrder> orders = new ArrayList<>();
        String query = "SELECT * FROM visit_order WHERE visitor_id = ?"; 
        
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, visitorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entities.VisitOrder order = new entities.VisitOrder(
                        rs.getInt("order_id"),
                        rs.getInt("park_id"),
                        rs.getString("visitor_id"),
                        rs.getString("visit_date"),
                        rs.getString("visit_time"),
                        rs.getInt("visitor_count"),
                        rs.getString("order_type"),
                        rs.getString("status"),
                        rs.getDouble("price") 
                    );
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching guest orders: " + e.getMessage());
        }
        return orders;
    }

    public static boolean cancelOrder(int orderId) {
        String getDetailsQuery = "SELECT park_id, visit_date, visit_time FROM visit_order WHERE order_id = ?";
        String cancelQuery = "UPDATE visit_order SET status = 'Cancelled' WHERE order_id = ?";
        
        int parkId = -1;
        String vDate = null;
        String vTime = null;

        try (java.sql.Connection conn = getInstance().getConnection()) {
            
            try (java.sql.PreparedStatement detailsStmt = conn.prepareStatement(getDetailsQuery)) {
                detailsStmt.setInt(1, orderId);
                try (java.sql.ResultSet rs = detailsStmt.executeQuery()) {
                    if (rs.next()) {
                        parkId = rs.getInt("park_id");
                        vDate = rs.getString("visit_date");
                        vTime = rs.getString("visit_time");
                    }
                }
            }

            try (java.sql.PreparedStatement cancelStmt = conn.prepareStatement(cancelQuery)) {
                cancelStmt.setInt(1, orderId);
                int rowsAffected = cancelStmt.executeUpdate();
                
                if (rowsAffected > 0 && parkId != -1) {
                    promoteFromWaitlist(parkId, vDate, vTime);
                    return true;
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Database error canceling order: " + e.getMessage());
        }
        return false;
    }

    private static void promoteFromWaitlist(int parkId, String date, String time) {
        try (java.sql.Connection conn = getInstance().getConnection()) {
            
            int maxCapacity = 0, casualGap = 0, currentBooked = 0;
            String parkQuery = "SELECT max_capacity, casual_gap FROM park WHERE park_id = ?";
            try (java.sql.PreparedStatement parkStmt = conn.prepareStatement(parkQuery)) {
                parkStmt.setInt(1, parkId);
                try (java.sql.ResultSet rs = parkStmt.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity = rs.getInt("max_capacity");
                        casualGap = rs.getInt("casual_gap");
                    }
                }
            }

            String sumQuery = "SELECT SUM(visitor_count) AS total_visitors FROM visit_order WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status IN ('Confirmed', 'In Park')";
            try (java.sql.PreparedStatement sumStmt = conn.prepareStatement(sumQuery)) {
                sumStmt.setInt(1, parkId);
                sumStmt.setString(2, date);
                sumStmt.setString(3, time);
                try (java.sql.ResultSet rs = sumStmt.executeQuery()) {
                    if (rs.next()) {
                        currentBooked = rs.getInt("total_visitors");
                    }
                }
            }

            int availableSpace = (maxCapacity - casualGap) - currentBooked;
            if (availableSpace <= 0) return; 

            String waitlistQuery = "SELECT order_id, visitor_count FROM visit_order WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status = 'Waitlisted' ORDER BY order_id ASC";
            try (java.sql.PreparedStatement waitStmt = conn.prepareStatement(waitlistQuery)) {
                waitStmt.setInt(1, parkId);
                waitStmt.setString(2, date);
                waitStmt.setString(3, time);
                
                try (java.sql.ResultSet rs = waitStmt.executeQuery()) {
                    while (rs.next()) {
                        int waitlistOrderId = rs.getInt("order_id");
                        int groupSize = rs.getInt("visitor_count");

                        if (groupSize <= availableSpace) {
                            String promoteQuery = "UPDATE visit_order SET status = 'Confirmed' WHERE order_id = ?";
                            try (java.sql.PreparedStatement promoteStmt = conn.prepareStatement(promoteQuery)) {
                                promoteStmt.setInt(1, waitlistOrderId);
                                promoteStmt.executeUpdate();
                                System.out.println(">>> WAITLIST ENGINE: Space opened up! Order #" + waitlistOrderId + " automatically promoted to Confirmed!");
                                availableSpace -= groupSize; 
                            }
                        }
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Waitlist Engine Error: " + e.getMessage());
        }
    }
}