package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import server.EmailSender;
import server.SmsSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DBController {

    private static DBController instance;
    private HikariDataSource dataSource;

    // 1. Constructor now requires the dynamic password
    private DBController(String dbPassword) {
        initPool(dbPassword);
    }

    // 2. New connect method called by the Start button on your UI
    public static synchronized boolean connect(String dbPassword) {
        if (instance == null) {
            instance = new DBController(dbPassword);
            if (instance.dataSource == null) {
                instance = null; // Reset if connection failed so user can try again
                return false;
            }
        }
        return true;
    }

    public static synchronized DBController getInstance() {
        if (instance == null) {
            System.err.println("CRITICAL: Database not connected!");
        }
        return instance;
    }

    // 3. Pool initialization using the password typed by the grader
    private void initPool(String dbPassword) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature_db?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword(dbPassword); // <--- DYNAMIC PASSWORD APPLIED HERE
            
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized successfully.");
            
            // --- BOOT THE AUTOMATED NOTIFICATION ENGINE ---
            startAutomatedNotificationEngine();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database pool: " + e.getMessage());
            dataSource = null; // Ensure the system knows the connection failed
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            instance = null; // Fully reset the instance so it can be reconnected later if needed
        }
    }

    // =========================================================================
    // --- 0. AUTOMATED NOTIFICATION ENGINE (REAL EMAIL INTEGRATION) ---
    // =========================================================================
    private void startAutomatedNotificationEngine() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // This background thread runs silently every 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = getConnection()) {
                
                // --- PHASE 1: Send 24-Hour Reminders ---
                String reminderQuery = "SELECT v.order_id, vis.first_name, vis.email, vis.phone FROM visit_order v " +
                                       "JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
                                       "WHERE v.status = 'Confirmed' AND v.notification_time IS NULL " +
                                       "AND TIMESTAMP(v.visit_date, v.visit_time) BETWEEN NOW() + INTERVAL 23 HOUR AND NOW() + INTERVAL 25 HOUR";
                
                String updateReminder = "UPDATE visit_order SET status = 'Pending Confirm', notification_time = NOW() WHERE order_id = ?";
                
                try (PreparedStatement checkStmt = conn.prepareStatement(reminderQuery);
                     ResultSet rs = checkStmt.executeQuery()) {
                    
                    while (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        String targetEmail = rs.getString("email");
                        String targetPhone = rs.getString("phone");
                        String fName = rs.getString("first_name");

                        System.out.println("⚠ SYSTEM ALERT: 24-Hour mark reached for Order #" + orderId + ". Triggering Notification Engine...");

                        if (targetEmail != null && targetEmail.contains("@")) {
                            String subject = "Action Required: Confirm your GoNature Visit (Order #" + orderId + ")";
                            String body = "Hello " + fName + ",\n\n"
                                        + "Your visit to GoNature is tomorrow!\n\n"
                                        + "Please log into the Guest Portal on our application and confirm your attendance.\n"
                                        + "IMPORTANT: You must confirm within 2 hours of receiving this email, or your ticket will be automatically canceled to make room for other guests.\n\n"
                                        + "See you soon,\nThe GoNature Team";
                            EmailSender.sendEmail(targetEmail, subject, body);
                        } else {
                            System.out.println(">>> Order #" + orderId + " has no valid email address. Skipping email delivery.");
                        }

                        SmsSender.sendSms(targetPhone,
                            "GoNature: Your visit is tomorrow! Open the app and confirm Order #" + orderId +
                            " within 2 hours or it will be auto-cancelled.");
                        
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateReminder)) {
                            updateStmt.setInt(1, orderId);
                            updateStmt.executeUpdate();
                        }
                    }
                }

                // --- PHASE 2: Auto-Cancel orders that ignored the message for 2 hours ---
                String cancelQuery = "SELECT order_id FROM visit_order WHERE status = 'Pending Confirm' AND notification_time <= NOW() - INTERVAL 2 HOUR";
                ArrayList<Integer> ordersToCancel = new ArrayList<>();
                
                try (PreparedStatement cancelStmt = conn.prepareStatement(cancelQuery);
                     ResultSet rs = cancelStmt.executeQuery()) {
                    while (rs.next()) {
                        ordersToCancel.add(rs.getInt("order_id"));
                    }
                }
                
                for (int expiredOrderId : ordersToCancel) {
                    System.out.println("⚠ SYSTEM ALERT: Order #" + expiredOrderId + " ignored the 2-hour confirmation window. Auto-canceling...");
                    cancelOrder(expiredOrderId);
                }

                // --- PHASE 3: Expire waitlist slots not confirmed within 1 hour ---
                String waitlistExpireQuery = "SELECT order_id FROM visit_order WHERE status = 'Waitlist Pending' AND notification_time <= NOW() - INTERVAL 1 HOUR";
                ArrayList<Integer> waitlistToExpire = new ArrayList<>();
                try (PreparedStatement expireStmt = conn.prepareStatement(waitlistExpireQuery);
                     ResultSet rs = expireStmt.executeQuery()) {
                    while (rs.next()) waitlistToExpire.add(rs.getInt("order_id"));
                }
                for (int expiredWaitlistId : waitlistToExpire) {
                    System.out.println("⚠ SYSTEM ALERT: Order #" + expiredWaitlistId + " missed the 1-hour waitlist window. Passing to next in line...");
                    cancelOrder(expiredWaitlistId);
                }

            } catch (Throwable e) {
                System.err.println("Notification Engine Error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
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
        String query = "SELECT order_id FROM visit_order WHERE visitor_id = ? AND park_id = ? AND visit_date = ? AND status IN ('Confirmed', 'Waitlisted', 'In Park', 'Pending Confirm')";
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
        String emailToSave = (order.getEmail() != null && !order.getEmail().isEmpty()) ? order.getEmail() : "Not Provided";
        String phoneToSave = (order.getPhone() != null && !order.getPhone().isEmpty()) ? order.getPhone() : "Not Provided";

        String ensureVisitorQuery = "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) " +
                                    "VALUES (?, 'Guest', 'Visitor', ?, ?, 'Regular') " +
                                    "ON DUPLICATE KEY UPDATE " +
                                    "email = IF(email='Not Provided', VALUES(email), email), " +
                                    "phone = IF(phone='Not Provided', VALUES(phone), phone)";
        
        try (Connection conn = getInstance().getConnection();
             PreparedStatement visitorStmt = conn.prepareStatement(ensureVisitorQuery)) {
            visitorStmt.setString(1, order.getVisitorId());
            visitorStmt.setString(2, emailToSave);
            visitorStmt.setString(3, phoneToSave);
            visitorStmt.executeUpdate(); 
        } catch (SQLException e) {
            System.err.println("Database Error: Could not create/update visitor profile - " + e.getMessage());
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

            String sumQuery = "SELECT SUM(visitor_count) AS total_visitors FROM visit_order WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status IN ('Confirmed', 'In Park', 'Pending Confirm', 'Waitlist Pending')";
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

            // Group booking requires a registered guide; reject if not
            if (order.getOrderType().equalsIgnoreCase("Group") && !isGuide) {
                return null; // Caller distinguishes null = DB error; use flag via special marker
            }

            if (isGuide || order.getOrderType().equalsIgnoreCase("Group")) {
                // Pre-booked group: 25% off + 12% advance-payment discount; guide goes free
                int payingVisitors = Math.max(0, order.getVisitorCount() - 1);
                calculatedPrice = payingVisitors * (basePrice * 0.75 * 0.88);
            } else if (isSubscriber) {
                // Pre-booked personal/family + subscriber: 15% off then extra 10% off
                calculatedPrice = order.getVisitorCount() * (basePrice * 0.85 * 0.90);
            } else {
                // Pre-booked personal/family: 15% off full price
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
    // --- 3. GATE ENTRY & EXIT ---
    // =========================================================================
    public static String processParkEntry(int orderId) {
        String selectQuery = "SELECT status, park_id, visitor_count FROM visit_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE visit_order SET status = 'In Park' WHERE order_id = ?";
        String updateParkQuery = "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";
        
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, orderId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        int parkId = rs.getInt("park_id");
                        int visitors = rs.getInt("visitor_count");
                        
                        if (status.equals("Confirmed")) {
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateOrderQuery)) {
                                updateStmt.setInt(1, orderId);
                                updateStmt.executeUpdate();
                            }
                            try (PreparedStatement parkStmt = conn.prepareStatement(updateParkQuery)) {
                                parkStmt.setInt(1, visitors);
                                parkStmt.setInt(2, parkId);
                                parkStmt.executeUpdate();
                            }
                            conn.commit(); 
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
    
 // =========================================================================
    // --- DEPARTMENT MANAGER: DIRECT PARK OVERRIDE ---
    // =========================================================================
    public static boolean forceUpdateParkParameters(entities.Park park) {
        String query = "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, park.getMaxCapacity());
            stmt.setInt(2, park.getCasualGap());
            stmt.setInt(3, park.getEstimatedStayTime());
            stmt.setInt(4, park.getParkId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("Database Error Forcing Park Update: " + e.getMessage());
            return false;
        }
    }

    public static String processParkExit(int orderId) {
        String selectQuery = "SELECT status, park_id, visitor_count FROM visit_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE visit_order SET status = 'Completed' WHERE order_id = ?";
        String updateParkQuery = "UPDATE park SET current_visitors = current_visitors - ? WHERE park_id = ?";
        
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, orderId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        int parkId = rs.getInt("park_id");
                        int visitors = rs.getInt("visitor_count");
                        
                        if (status.equals("In Park")) {
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateOrderQuery)) {
                                updateStmt.setInt(1, orderId);
                                updateStmt.executeUpdate();
                            }
                            try (PreparedStatement parkStmt = conn.prepareStatement(updateParkQuery)) {
                                parkStmt.setInt(1, visitors);
                                parkStmt.setInt(2, parkId);
                                parkStmt.executeUpdate();
                            }
                            conn.commit();
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
    // --- 6. SERVICE REPRESENTATIVE REGISTRATION ---
    // =========================================================================
    public static String registerNewSubscriber(entities.Subscriber sub) {
        try (java.sql.Connection conn = getInstance().getConnection()) {

            String checkSubQuery = "SELECT subscriber_id FROM subscriber WHERE visitor_id = ?";
            try (java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSubQuery)) {
                checkStmt.setString(1, sub.getVisitorId());
                try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return "Error: ID " + sub.getVisitorId() + " is already a registered subscriber.";
                    }
                }
            }

            String visType = sub.isGuide() ? "Guide" : "Subscriber";

            String checkVisQuery = "SELECT visitor_id FROM visitor WHERE visitor_id = ?";
            boolean visitorExists = false;
            try (java.sql.PreparedStatement checkVisStmt = conn.prepareStatement(checkVisQuery)) {
                checkVisStmt.setString(1, sub.getVisitorId());
                try (java.sql.ResultSet rs = checkVisStmt.executeQuery()) {
                    if (rs.next()) visitorExists = true;
                }
            }

            if (visitorExists) {
                String updateVis = "UPDATE visitor SET first_name=?, last_name=?, email=?, phone=?, visitor_type=? WHERE visitor_id=?";
                try (java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateVis)) {
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
                try (java.sql.PreparedStatement insertStmt = conn.prepareStatement(insertVis)) {
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
            try (java.sql.PreparedStatement insertSubStmt = conn.prepareStatement(insertSubQuery, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insertSubStmt.setString(1, sub.getVisitorId());
                insertSubStmt.setInt(2, sub.getFamilySize());
                insertSubStmt.setString(3, sub.getCreditCard());
                insertSubStmt.setBoolean(4, sub.isGuide());
                insertSubStmt.executeUpdate();

                try (java.sql.ResultSet generatedKeys = insertSubStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newSubId = generatedKeys.getInt(1);
                        return "SUCCESS: " + visType + " registered! (Sub ID: #" + newSubId + ")";
                    }
                }
            }

        } catch (java.sql.SQLException e) {
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
                    orders.add(new entities.VisitOrder(
                        rs.getInt("order_id"), rs.getInt("park_id"), rs.getString("visitor_id"),
                        rs.getString("visit_date"), rs.getString("visit_time"), rs.getInt("visitor_count"),
                        rs.getString("order_type"), rs.getString("status"), rs.getDouble("price") 
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static boolean confirmOrder(int orderId) {
        String query = "UPDATE visit_order SET status = 'Confirmed' WHERE order_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            return stmt.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean cancelOrder(int orderId) {
        String getDetailsQuery = "SELECT park_id, visit_date, visit_time FROM visit_order WHERE order_id = ?";
        String cancelQuery = "UPDATE visit_order SET status = 'Cancelled' WHERE order_id = ?";
        int parkId = -1; String vDate = null; String vTime = null;

        try (java.sql.Connection conn = getInstance().getConnection()) {
            try (java.sql.PreparedStatement detailsStmt = conn.prepareStatement(getDetailsQuery)) {
                detailsStmt.setInt(1, orderId);
                try (java.sql.ResultSet rs = detailsStmt.executeQuery()) {
                    if (rs.next()) {
                        parkId = rs.getInt("park_id"); vDate = rs.getString("visit_date"); vTime = rs.getString("visit_time");
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
            e.printStackTrace();
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
                    if (rs.next()) { maxCapacity = rs.getInt("max_capacity"); casualGap = rs.getInt("casual_gap"); }
                }
            }

            String sumQuery = "SELECT SUM(visitor_count) AS total_visitors FROM visit_order WHERE park_id = ? AND visit_date = ? AND visit_time = ? AND status IN ('Confirmed', 'In Park', 'Pending Confirm', 'Waitlist Pending')";
            try (java.sql.PreparedStatement sumStmt = conn.prepareStatement(sumQuery)) {
                sumStmt.setInt(1, parkId); sumStmt.setString(2, date); sumStmt.setString(3, time);
                try (java.sql.ResultSet rs = sumStmt.executeQuery()) {
                    if (rs.next()) { currentBooked = rs.getInt("total_visitors"); }
                }
            }

            int availableSpace = (maxCapacity - casualGap) - currentBooked;
            if (availableSpace <= 0) return; 

            String waitlistQuery = "SELECT v.order_id, v.visitor_count, vis.first_name, vis.email, vis.phone " +
                                   "FROM visit_order v JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
                                   "WHERE v.park_id = ? AND v.visit_date = ? AND v.visit_time = ? AND v.status = 'Waitlisted' " +
                                   "ORDER BY v.order_id ASC";
                                   
            try (java.sql.PreparedStatement waitStmt = conn.prepareStatement(waitlistQuery)) {
                waitStmt.setInt(1, parkId); waitStmt.setString(2, date); waitStmt.setString(3, time);
                try (java.sql.ResultSet rs = waitStmt.executeQuery()) {
                    while (rs.next()) {
                        int waitlistOrderId = rs.getInt("order_id");
                        int groupSize = rs.getInt("visitor_count");
                        String targetEmail = rs.getString("email");
                        String targetPhone = rs.getString("phone");
                        String fName = rs.getString("first_name");

                        if (groupSize <= availableSpace) {
                            // Promote to 'Waitlist Pending' — visitor has 1 hour to confirm
                            String promoteQuery = "UPDATE visit_order SET status = 'Waitlist Pending', notification_time = NOW() WHERE order_id = ?";
                            try (java.sql.PreparedStatement promoteStmt = conn.prepareStatement(promoteQuery)) {
                                promoteStmt.setInt(1, waitlistOrderId);
                                promoteStmt.executeUpdate();

                                System.out.println("⚠ SYSTEM ALERT: Order #" + waitlistOrderId + " promoted from waitlist. 1-hour confirmation window started.");

                                if (targetEmail != null && targetEmail.contains("@")) {
                                    String subject = "A spot opened up! Confirm your GoNature visit within 1 hour (Order #" + waitlistOrderId + ")";
                                    String body = "Hello " + fName + ",\n\n"
                                                + "Great news! A spot has opened at GoNature on " + date + " at " + time + ".\n\n"
                                                + "Your waitlisted order has been selected. Please log into the Guest Portal and confirm your visit.\n"
                                                + "IMPORTANT: You must confirm within 1 hour or the spot will be passed to the next person in line.\n\n"
                                                + "The GoNature Team";
                                    EmailSender.sendEmail(targetEmail, subject, body);
                                }

                                SmsSender.sendSms(targetPhone,
                                    "GoNature: A spot opened for Order #" + waitlistOrderId +
                                    " on " + date + " at " + time + ". Open the Guest Portal and confirm within 1 hour!");

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
    
    // =========================================================================
    // --- 8. STATISTICAL REPORTING ENGINE ---
    // =========================================================================
    public static entities.ReportData generateMonthlyReport(int parkId, String month, String year) {
        entities.ReportData report = new entities.ReportData(parkId, month, year);
        
        String query = "SELECT order_type, SUM(visitor_count) as total_visitors, SUM(price) as total_income " +
                       "FROM visit_order " +
                       "WHERE park_id = ? AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
                       "AND status IN ('Completed', 'In Park') " +
                       "GROUP BY order_type";

        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, parkId);
            stmt.setInt(2, Integer.parseInt(month));
            stmt.setInt(3, Integer.parseInt(year));
            
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("order_type");
                    int visitors = rs.getInt("total_visitors");
                    double income = rs.getDouble("total_income");
                    
                    report.addVisitorData(type, visitors);
                    report.addIncomeData(type, income);
                }
            }
            return report;
        } catch (java.sql.SQLException e) {
            System.err.println("Reporting Engine Error: " + e.getMessage());
            return null;
        }
    }
    
    // =========================================================================
    // --- 9. GUIDE VERIFICATION ---
    // =========================================================================
    public static boolean isRegisteredGuide(String visitorId) {
        String query = "SELECT is_guide FROM subscriber WHERE visitor_id = ? AND is_guide = true";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, visitorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // --- 10. WALK-IN VISITOR ENGINE ---
    // =========================================================================
    public static entities.VisitOrder processWalkIn(int parkId, int visitorCount, String orderType) {
        try (Connection conn = getInstance().getConnection()) {
            int maxCapacity = 0, currentVisitors = 0;
            String parkQuery = "SELECT max_capacity, current_visitors FROM park WHERE park_id = ?";
            try (PreparedStatement parkStmt = conn.prepareStatement(parkQuery)) {
                parkStmt.setInt(1, parkId);
                try (ResultSet rs = parkStmt.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity = rs.getInt("max_capacity");
                        currentVisitors = rs.getInt("current_visitors");
                    }
                }
            }

            int availableNow = maxCapacity - currentVisitors;
            if (availableNow < visitorCount) {
                return null; // No space
            }

            // Walk-in pricing: full price for personal/family; 10% off for walk-in group (guide pays)
            int basePrice = 100;
            double price;
            if (orderType.equalsIgnoreCase("Group")) {
                price = visitorCount * (basePrice * 0.90);
            } else {
                price = visitorCount * basePrice;
            }

            String today = java.time.LocalDate.now().toString();
            String now = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ":00";

            // Generate a unique walk-in visitor ID
            String walkInId = "WI" + (System.currentTimeMillis() % 1000000);

            String ensureVisitor = "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) " +
                                   "VALUES (?, 'Walk-in', 'Visitor', 'Not Provided', 'Not Provided', 'Regular') " +
                                   "ON DUPLICATE KEY UPDATE visitor_id=visitor_id";
            try (PreparedStatement visStmt = conn.prepareStatement(ensureVisitor)) {
                visStmt.setString(1, walkInId);
                visStmt.executeUpdate();
            }

            String insertQuery = "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, " +
                                 "visitor_count, order_type, status, price) VALUES (?, ?, ?, ?, ?, ?, 'In Park', ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, parkId);
                insertStmt.setString(2, walkInId);
                insertStmt.setString(3, today);
                insertStmt.setString(4, now);
                insertStmt.setInt(5, visitorCount);
                insertStmt.setString(6, orderType);
                insertStmt.setDouble(7, price);
                insertStmt.executeUpdate();

                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        String updatePark = "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";
                        try (PreparedStatement parkUpd = conn.prepareStatement(updatePark)) {
                            parkUpd.setInt(1, visitorCount);
                            parkUpd.setInt(2, parkId);
                            parkUpd.executeUpdate();
                        }
                        return new entities.VisitOrder(newId, parkId, walkInId, today, now,
                                                       visitorCount, orderType, "In Park", price);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // --- 11. DEPARTMENT MANAGER REPORTS ---
    // =========================================================================
    public static ArrayList<entities.Park> getAllParks() {
        ArrayList<entities.Park> parks = new ArrayList<>();
        String query = "SELECT * FROM park";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                parks.add(new entities.Park(
                    rs.getInt("park_id"), rs.getString("name"),
                    rs.getInt("max_capacity"), rs.getInt("casual_gap"),
                    rs.getInt("estimated_stay_time"), rs.getInt("current_visitors")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parks;
    }

    public static entities.ReportData getDeptMonthlyReport(int parkId, String month, String year) {
        String query = "SELECT * FROM monthly_reports WHERE park_id = ? AND report_month = ? AND report_year = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, parkId);
            stmt.setString(2, month);
            stmt.setString(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    entities.ReportData report = new entities.ReportData(parkId, month, year);
                    report.addVisitorData("Solo", rs.getInt("solo_visitors"));
                    report.addVisitorData("Family", rs.getInt("family_visitors"));
                    report.addVisitorData("Group", rs.getInt("group_visitors"));
                    report.addVisitorData("Subscriber", rs.getInt("subscriber_visitors"));
                    report.addIncomeData("Income", rs.getDouble("total_income"));
                    return report;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static entities.ReportData getCancellationsReport(String month, String year) {
        entities.ReportData report = new entities.ReportData(0, month, year);
        String query = "SELECT order_type, COUNT(*) as cnt FROM visit_order " +
                       "WHERE status = 'Cancelled' AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
                       "GROUP BY order_type";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(month));
            stmt.setInt(2, Integer.parseInt(year));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    report.addVisitorData(rs.getString("order_type"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }

    // =========================================================================
    // --- 12. AVAILABLE SLOTS (for rejected/waitlisted bookings) ---
    // =========================================================================
    public static ArrayList<String> getAvailableSlots(int parkId, String date) {
        ArrayList<String> slots = new ArrayList<>();
        String[] allSlots = {"08:00:00","09:00:00","10:00:00","11:00:00","12:00:00",
                             "13:00:00","14:00:00","15:00:00","16:00:00","17:00:00"};
        try (Connection conn = getInstance().getConnection()) {
            int maxCapacity = 0, casualGap = 0;
            String parkQ = "SELECT max_capacity, casual_gap FROM park WHERE park_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(parkQ)) {
                ps.setInt(1, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { maxCapacity = rs.getInt("max_capacity"); casualGap = rs.getInt("casual_gap"); }
                }
            }
            int available = maxCapacity - casualGap;
            for (String slot : allSlots) {
                String sumQ = "SELECT COALESCE(SUM(visitor_count),0) as booked FROM visit_order " +
                              "WHERE park_id=? AND visit_date=? AND visit_time=? " +
                              "AND status IN ('Confirmed','In Park','Pending Confirm')";
                try (PreparedStatement ps = conn.prepareStatement(sumQ)) {
                    ps.setInt(1, parkId); ps.setString(2, date); ps.setString(3, slot);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int free = available - rs.getInt("booked");
                            if (free > 0) slots.add(slot.substring(0, 5) + " (" + free + " spots free)");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }

    public static String saveMonthlyReport(entities.ReportData report) {
        int solo = report.getVisitorBreakdown().getOrDefault("Solo", 0);
        int family = report.getVisitorBreakdown().getOrDefault("Family", 0);
        int group = report.getVisitorBreakdown().getOrDefault("Group", 0);
        int subscriber = report.getVisitorBreakdown().getOrDefault("Subscriber", 0);

        String query = "INSERT INTO monthly_reports (park_id, report_month, report_year, total_visitors, total_income, solo_visitors, family_visitors, group_visitors, subscriber_visitors) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE " +
                       "total_visitors=VALUES(total_visitors), total_income=VALUES(total_income), " +
                       "solo_visitors=VALUES(solo_visitors), family_visitors=VALUES(family_visitors), " +
                       "group_visitors=VALUES(group_visitors), subscriber_visitors=VALUES(subscriber_visitors)";

        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, report.getParkId());
            stmt.setString(2, report.getMonth());
            stmt.setString(3, report.getYear());
            stmt.setInt(4, report.getTotalVisitors());
            stmt.setDouble(5, report.getTotalIncome());
            stmt.setInt(6, solo);
            stmt.setInt(7, family);
            stmt.setInt(8, group);
            stmt.setInt(9, subscriber);
            
            stmt.executeUpdate();
            return "SUCCESS: Report successfully submitted to the Department Manager.";

        } catch (java.sql.SQLException e) {
            System.err.println("Database Error Saving Report: " + e.getMessage());
            return "ERROR: Could not save the report to the database.";
        }
    }

    public static int getOrderParkId(int orderId) {
        String query = "SELECT park_id FROM visit_order WHERE order_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("park_id");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // =========================================================================
    // --- 13. USAGE REPORT (below max capacity) ---
    // =========================================================================
    public static ArrayList<String[]> getUsageReport(int parkId, int month, int year) {
        ArrayList<String[]> rows = new ArrayList<>();
        String query =
            "SELECT vo.visit_date, COALESCE(SUM(vo.visitor_count), 0) AS daily_total, p.max_capacity " +
            "FROM visit_order vo " +
            "JOIN park p ON vo.park_id = p.park_id " +
            "WHERE vo.park_id = ? AND MONTH(vo.visit_date) = ? AND YEAR(vo.visit_date) = ? " +
            "  AND vo.status IN ('Confirmed','In Park','Completed','Pending Confirm','Waitlist Pending') " +
            "GROUP BY vo.visit_date, p.max_capacity " +
            "HAVING daily_total < p.max_capacity " +
            "ORDER BY vo.visit_date";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, parkId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String date      = rs.getString("visit_date");
                    int    visitors  = rs.getInt("daily_total");
                    int    maxCap    = rs.getInt("max_capacity");
                    int    available = maxCap - visitors;
                    double pct       = maxCap > 0 ? (visitors * 100.0 / maxCap) : 0;
                    rows.add(new String[]{ date, String.valueOf(visitors), String.valueOf(maxCap),
                                          String.valueOf(available), String.format("%.1f", pct) });
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }
}