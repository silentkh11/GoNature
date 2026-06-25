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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton database controller for GoNature.
 * Manages all DB operations via a HikariCP connection pool.
 */
public class DBController {

    /** Base URL for one-click email confirmation links — set by EchoServer on start. */
    public static String serverBaseUrl = "http://127.0.0.1:8080";

    private static DBController instance;
    private HikariDataSource dataSource;
    private ScheduledExecutorService notificationScheduler;

    private DBController(String dbPassword) {
        initPool(dbPassword);
    }

    /** Initialises the connection pool. Called once from the server UI Start button. */
    public static synchronized boolean connect(String dbPassword) {
        if (instance == null) {
            instance = new DBController(dbPassword);
            if (instance.dataSource == null) {
                instance = null;
                return false;
            }
        }
        return true;
    }

    /** Returns the singleton instance. */
    public static synchronized DBController getInstance() {
        if (instance == null) {
            System.err.println("CRITICAL: Database not connected!");
        }
        return instance;
    }

    private void initPool(String dbPassword) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature_db?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);

            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized successfully.");

            ensureSchema();
            startAutomatedNotificationEngine();

        } catch (Exception e) {
            System.err.println("Failed to initialize database pool: " + e.getMessage());
            dataSource = null;
        }
    }

    /**
     * Applies any missing schema migrations so the DB is always up to date.
     * Checks INFORMATION_SCHEMA before each ALTER so it is safe on any MySQL 8.x version.
     */
    private void ensureSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {

            addColumnIfMissing(st, "visit_order", "entry_timestamp", "TIMESTAMP NULL DEFAULT NULL");
            addColumnIfMissing(st, "visit_order", "exit_timestamp",  "TIMESTAMP NULL DEFAULT NULL");
            addColumnIfMissing(st, "park",        "active_discount", "DECIMAL(5,2) NOT NULL DEFAULT 0");
            // created_at: guards the 24h reminder from firing on same-day/next-day bookings
            addColumnIfMissing(st, "visit_order", "created_at",        "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing(st, "visit_order", "notification_time", "TIMESTAMP NULL DEFAULT NULL");
            // email + phone on the order itself so reminders always reach the booker,
            // regardless of what email the visitor table holds for that visitor_id.
            addColumnIfMissing(st, "visit_order", "email", "VARCHAR(255) NULL DEFAULT NULL");
            addColumnIfMissing(st, "visit_order", "phone", "VARCHAR(20)  NULL DEFAULT NULL");

            st.execute(
                "CREATE TABLE IF NOT EXISTS park_promotion (" +
                "  promotion_id     INT AUTO_INCREMENT PRIMARY KEY," +
                "  park_id          INT          NOT NULL," +
                "  park_name        VARCHAR(100) NOT NULL," +
                "  discount_percent DECIMAL(5,2) NOT NULL," +
                "  status           VARCHAR(20)  NOT NULL DEFAULT 'Pending'," +
                "  created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Seed additional parks — INSERT IGNORE skips rows whose park_id already exists
            st.execute(
                "INSERT IGNORE INTO park (park_id, name, max_capacity, casual_gap, estimated_stay_time, current_visitors, active_discount) VALUES " +
                "(5, 'Masada National Park',       800, 80, 3, 0, 0)," +
                "(6, 'Caesarea National Park',     600, 60, 2, 0, 0)," +
                "(7, 'Timna Valley Park',          400, 40, 3, 0, 0)," +
                "(8, 'Nahal Ayun Nature Reserve',  200, 20, 2, 0, 0)"
            );

            System.out.println("Schema verified/migrated successfully.");
        } catch (SQLException e) {
            System.err.println("Schema migration error: " + e.getMessage());
        }
    }

    /** Adds a column only if it does not already exist — works on all MySQL 8.x versions. */
    private void addColumnIfMissing(Statement st, String table, String column, String definition) throws SQLException {
        String check =
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = st.getConnection().prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    System.out.println("Schema: added column " + table + "." + column);
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Shuts down the notification scheduler and connection pool cleanly. */
    public void closePool() {
        if (notificationScheduler != null && !notificationScheduler.isShutdown()) {
            notificationScheduler.shutdownNow();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            instance = null;
        }
    }

    // =========================================================================
    // --- 0. AUTOMATED NOTIFICATION ENGINE ---
    // =========================================================================
    private void startAutomatedNotificationEngine() {
        notificationScheduler = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService scheduler = notificationScheduler;

        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = getConnection()) {

                // PHASE 1: Send 24-Hour Reminders
                // Window  : visit is 23h58m–24h00m away (2-min slot at the exact 24h mark).
                // Guard   : booking was made BEFORE the 24h mark (created_at < visit − 24h).
                // Result  : reminder fires at the 24h mark regardless of when the booking
                //           was made, as long as the booking itself was before that mark.
                //           notification_time IS NULL ensures it fires exactly once.
                // email + phone come from visit_order (stored at booking time) so reminders
                // always go to the address the booker typed, not the visitor profile's address.
                String reminderQuery =
                    "SELECT v.order_id, v.visit_date, v.visit_time, v.visitor_count, " +
                    "vis.first_name, v.email, v.phone FROM visit_order v " +
                    "JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
                    "WHERE v.status = 'Booked' AND v.notification_time IS NULL " +
                    "AND TIMESTAMP(v.visit_date, v.visit_time) BETWEEN NOW() + INTERVAL 1438 MINUTE AND NOW() + INTERVAL 1440 MINUTE " +
                    "AND v.created_at < TIMESTAMP(v.visit_date, v.visit_time) - INTERVAL 1440 MINUTE";
                String updateReminder =
                    "UPDATE visit_order SET status = 'Pending Confirm', notification_time = NOW() WHERE order_id = ?";

                // Step 1 — collect all due orders into a list before touching the DB.
                // This closes the ResultSet before any UPDATEs run, avoiding cursor
                // contamination on the shared connection.
                List<Map<String, Object>> dueOrders = new ArrayList<>();
                try (PreparedStatement checkStmt = conn.prepareStatement(reminderQuery);
                     ResultSet rs = checkStmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("orderId",      rs.getInt("order_id"));
                        row.put("visitDate",    rs.getString("visit_date"));
                        row.put("visitTime",    rs.getString("visit_time"));
                        row.put("visitorCount", rs.getInt("visitor_count"));
                        row.put("email",        rs.getString("email"));
                        row.put("phone",        rs.getString("phone"));
                        row.put("fName",        rs.getString("first_name"));
                        dueOrders.add(row);
                    }
                }
                // ResultSet is now fully closed.

                // Step 2 — lock every order in the DB first (status → Pending Confirm,
                // notification_time = NOW()).  Doing this before sending any notification
                // prevents a second scheduler tick from re-picking the same orders if
                // email sending happens to take longer than expected.
                // Log all SYSTEM ALERTs here so the console shows them together, in order,
                // uninterrupted by concurrent OCSF-thread emails (booking confirmations, etc.).
                for (Map<String, Object> row : dueOrders) {
                    int    orderId = (int) row.get("orderId");
                    String email   = (String) row.get("email");
                    String confirmUrl = serverBaseUrl + "/confirm?id=" + orderId;
                    String cancelUrl  = serverBaseUrl + "/cancel?id="  + orderId;
                    row.put("confirmUrl", confirmUrl);
                    row.put("cancelUrl",  cancelUrl);

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateReminder)) {
                        updateStmt.setInt(1, orderId);
                        updateStmt.executeUpdate();
                    }
                    System.out.println("⚠ SYSTEM ALERT: 24-Hour Reminder — Order #" + orderId
                        + (email != null ? "  →  " + email : "  (no email on file)"));
                    System.out.println("  ↳ CONFIRM: " + confirmUrl);
                    System.out.println("  ↳ CANCEL:  " + cancelUrl);
                }

                // Step 3 — now send the actual notifications.  DB is already updated so
                // failures here don't cause duplicate reminders.
                for (Map<String, Object> row : dueOrders) {
                    int    orderId      = (int)    row.get("orderId");
                    String visitDate    = (String) row.get("visitDate");
                    String visitTime    = (String) row.get("visitTime");
                    int    visitorCount = (int)    row.get("visitorCount");
                    String email        = (String) row.get("email");
                    String phone        = (String) row.get("phone");
                    String fName        = (String) row.get("fName");
                    String confirmUrl   = (String) row.get("confirmUrl");
                    String cancelUrl    = (String) row.get("cancelUrl");
                    String shortTime    = visitTime != null ? visitTime.substring(0, Math.min(5, visitTime.length())) : "";

                    if (email != null && email.contains("@")) {
                        String subject  = "⚠ Action Required: Confirm your GoNature Visit (Order #" + orderId + ")";
                        String htmlBody = buildReminderEmail(fName, orderId, visitDate, shortTime,
                                                             visitorCount, confirmUrl, cancelUrl);
                        EmailSender.sendHtmlEmail(email, subject, htmlBody);
                    }
                    SmsSender.sendSms(phone,
                        "GoNature: Visit tomorrow! Confirm Order #" + orderId + " within 2h: " + confirmUrl);
                }

                // PHASE 2: Auto-Cancel orders that ignored the 2-hour window
                String cancelQuery =
                    "SELECT order_id FROM visit_order WHERE status = 'Pending Confirm' " +
                    "AND notification_time <= NOW() - INTERVAL 2 HOUR";
                ArrayList<Integer> ordersToCancel = new ArrayList<>();
                try (PreparedStatement st = conn.prepareStatement(cancelQuery);
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) ordersToCancel.add(rs.getInt("order_id"));
                }
                for (int id : ordersToCancel) {
                    System.out.println("⚠ SYSTEM ALERT: Order #" + id + " auto-cancelled (2-hour window expired).");
                    cancelOrder(id);
                }

                // PHASE 3: Expire waitlist slots not confirmed within 1 hour
                String expireQuery =
                    "SELECT order_id FROM visit_order WHERE status = 'Waitlist Pending' " +
                    "AND notification_time <= NOW() - INTERVAL 1 HOUR";
                ArrayList<Integer> toExpire = new ArrayList<>();
                try (PreparedStatement st = conn.prepareStatement(expireQuery);
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) toExpire.add(rs.getInt("order_id"));
                }
                for (int id : toExpire) {
                    System.out.println("⚠ SYSTEM ALERT: Waitlist Order #" + id + " expired (1-hour window).");
                    cancelOrder(id);
                }

            } catch (Throwable e) {
                System.err.println("Notification Engine Error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    // =========================================================================
    // --- 1. LOGIN ---
    // =========================================================================
    /**
     * Verifies employee credentials.
     * @return Employee object if valid, null otherwise.
     */
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

    /** Returns true if the visitor already has an active booking for this park on this date. */
    public static boolean hasDuplicateBooking(entities.VisitOrder order) {
        String query =
            "SELECT order_id FROM visit_order WHERE visitor_id = ? AND park_id = ? AND visit_date = ? " +
            "AND status IN ('Booked', 'Confirmed', 'Waitlisted', 'In Park', 'Pending Confirm', 'Waitlist Pending')";
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

    /**
     * Processes a new visitor booking request.
     * Applies capacity logic, pricing (with active promotional discount),
     * and assigns Confirmed or Waitlisted status.
     * Returns null if rejected (guide required for groups, DB error).
     */
    public static entities.VisitOrder processNewOrder(entities.VisitOrder order) {
        String emailToSave = (order.getEmail() != null && !order.getEmail().isEmpty()) ? order.getEmail() : "Not Provided";
        String phoneToSave = (order.getPhone() != null && !order.getPhone().isEmpty()) ? order.getPhone() : "Not Provided";

        String ensureVisitorQuery =
            "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) " +
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
            System.err.println("Database Error: Could not create/update visitor profile — " + e.getMessage());
            return null;
        }

        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false);
            int maxCapacity = 0, casualGap = 0, currentBooked = 0, estimatedStayTime = 4;
            double activeDiscount = 0;

            // FOR UPDATE locks the park row so concurrent bookings for the same park
            // cannot both pass the capacity check before either inserts its order.
            String parkQuery = "SELECT max_capacity, casual_gap, active_discount, estimated_stay_time FROM park WHERE park_id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(parkQuery)) {
                ps.setInt(1, order.getParkId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity       = rs.getInt("max_capacity");
                        casualGap         = rs.getInt("casual_gap");
                        activeDiscount    = rs.getDouble("active_discount");
                        int stay          = rs.getInt("estimated_stay_time");
                        if (stay > 0) estimatedStayTime = stay;
                    }
                }
            }

            // Count any existing booking whose stay window overlaps the requested visit_time.
            String sumQuery =
                "SELECT COALESCE(SUM(visitor_count), 0) AS total_visitors FROM visit_order " +
                "WHERE park_id = ? AND visit_date = ? " +
                "AND HOUR(visit_time) > HOUR(?) - ? " +
                "AND HOUR(visit_time) < HOUR(?) + ? " +
                "AND status IN ('Booked', 'Confirmed', 'In Park', 'Pending Confirm', 'Waitlist Pending')";
            try (PreparedStatement ps = conn.prepareStatement(sumQuery)) {
                ps.setInt(1, order.getParkId());
                ps.setString(2, order.getVisitDate());
                ps.setString(3, order.getVisitTime());
                ps.setInt(4, estimatedStayTime);
                ps.setString(5, order.getVisitTime());
                ps.setInt(6, estimatedStayTime);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) currentBooked = rs.getInt("total_visitors");
                }
            }

            int availableForPreorder = maxCapacity - casualGap;
            String assignedStatus = ((currentBooked + order.getVisitorCount()) > availableForPreorder)
                ? "Waitlisted" : "Booked";

            int basePrice = 100;
            boolean isSubscriber = false;
            boolean isGuide = false;

            String subQuery = "SELECT is_guide FROM subscriber WHERE visitor_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(subQuery)) {
                ps.setString(1, order.getVisitorId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        isSubscriber = true;
                        isGuide = rs.getBoolean("is_guide");
                    }
                }
            }

            // Group booking requires a registered guide
            if (order.getOrderType().equalsIgnoreCase("Group") && !isGuide) {
                return null;
            }

            double calculatedPrice;
            if (isGuide || order.getOrderType().equalsIgnoreCase("Group")) {
                // Pre-booked group: 25% off + 12% advance; guide goes free
                int payingVisitors = Math.max(0, order.getVisitorCount() - 1);
                calculatedPrice = payingVisitors * (basePrice * 0.75 * 0.88);
                // Subscriber guide also gets the additional 10% off (spec: cumulative discounts)
                if (isSubscriber) calculatedPrice *= 0.90;
            } else if (isSubscriber) {
                // Pre-booked personal/family + subscriber: -15% then -10%
                calculatedPrice = order.getVisitorCount() * (basePrice * 0.85 * 0.90);
            } else {
                // Pre-booked personal/family: -15%
                calculatedPrice = order.getVisitorCount() * (basePrice * 0.85);
            }

            // Apply active promotional discount (if any) on top of base pricing
            if (activeDiscount > 0) {
                calculatedPrice = calculatedPrice * (1.0 - activeDiscount / 100.0);
            }

            order.setPrice(calculatedPrice);

            String insertQuery =
                "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, visitor_count, order_type, status, price, email, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getParkId());
                ps.setString(2, order.getVisitorId());
                ps.setString(3, order.getVisitDate());
                ps.setString(4, order.getVisitTime());
                ps.setInt(5, order.getVisitorCount());
                ps.setString(6, order.getOrderType());
                ps.setString(7, assignedStatus);
                ps.setDouble(8, calculatedPrice);
                ps.setString(9, order.getEmail());
                ps.setString(10, order.getPhone());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        order.setOrderId(keys.getInt(1));
                        order.setStatus(assignedStatus);
                        conn.commit();

                        // Send booking confirmation email + SMS immediately on booking
                        if (assignedStatus.equals("Booked")) {
                            String cancelUrl = serverBaseUrl + "/cancel?id=" + order.getOrderId();
                            String shortTime = (order.getVisitTime() != null)
                                ? order.getVisitTime().substring(0, Math.min(5, order.getVisitTime().length()))
                                : "";
                            try {
                                if (order.getEmail() != null && order.getEmail().contains("@")) {
                                    String subject = "Your GoNature visit details — Order #" + order.getOrderId();
                                    String htmlBody = buildBookingConfirmEmail(
                                        order.getOrderId(), order.getVisitDate(), shortTime,
                                        order.getVisitorCount(), calculatedPrice, cancelUrl);
                                    EmailSender.sendHtmlEmail(order.getEmail(), subject, htmlBody);
                                }
                            } catch (Throwable mailEx) {
                                System.err.println("Booking confirmation email failed: " + mailEx.getMessage());
                            }
                            try {
                                if (order.getPhone() != null && !order.getPhone().isBlank()) {
                                    SmsSender.sendSms(order.getPhone(),
                                        "GoNature: Booking confirmed! Order #" + order.getOrderId()
                                        + " for " + order.getVisitDate() + " at " + shortTime
                                        + ". A reminder will arrive 24h before your visit.");
                                }
                            } catch (Throwable smsEx) {
                                System.err.println("Booking confirmation SMS failed: " + smsEx.getMessage());
                            }
                        }

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

    /**
     * Records a visitor entry: sets status to 'In Park', increments park count,
     * and stamps the entry_timestamp.
     */
 // =========================================================================
    // --- PARK ENTRY & GATE MANAGEMENT ---
    // =========================================================================
    public static String processParkEntry(int orderId) {
        String fetchOrderQuery =
            "SELECT park_id, visitor_count, status, price, order_type, visit_date, visit_time " +
            "FROM visit_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE visit_order SET status = 'In Park', entry_timestamp = NOW() WHERE order_id = ?";
        String updateParkQuery = "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";

        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement fetchStmt = conn.prepareStatement(fetchOrderQuery)) {

            fetchStmt.setInt(1, orderId);
            try (java.sql.ResultSet rs = fetchStmt.executeQuery()) {
                if (!rs.next()) {
                    return "DENIED:Order ID #" + orderId + " not found in the database.";
                }

                String status    = rs.getString("status");
                int visitors     = rs.getInt("visitor_count");
                int parkId       = rs.getInt("park_id");
                double price     = rs.getDouble("price");
                String orderType = rs.getString("order_type");
                String visitDate = rs.getString("visit_date");
                String visitTime = rs.getString("visit_time");

                // 1. Validate Order Status
                if (status.equals("In Park") || status.equals("Completed")) {
                    return "DENIED:Order #" + orderId + " has already been scanned.";
                }
                if (status.equals("Cancelled") || status.equals("Expired")) {
                    return "DENIED:Order #" + orderId + " was cancelled or expired.";
                }
                if (status.equals("Waitlisted")) {
                    return "DENIED:Order #" + orderId + " is on the waitlist and has not been confirmed yet.";
                }

                // 2. Date validation: entry only allowed on the exact booked day
                java.time.LocalDate today    = java.time.LocalDate.now();
                java.time.LocalDate bookedDay;
                try {
                    bookedDay = java.time.LocalDate.parse(visitDate.substring(0, 10));
                } catch (Exception ex) {
                    return "DENIED:Cannot verify the visit date for Order #" + orderId + ".";
                }
                if (bookedDay.isAfter(today)) {
                    return "DENIED:This booking is for " + visitDate.substring(0, 10)
                        + ". Entry is only permitted on the day of your visit.";
                }
                if (bookedDay.isBefore(today)) {
                    return "DENIED:This booking was for " + visitDate.substring(0, 10)
                        + " and has already expired. Please contact the park service desk.";
                }

                // 3. Time validation: entry only allowed at or after the booked visit time
                if (visitTime != null && visitTime.length() >= 5) {
                    try {
                        java.time.LocalTime booked = java.time.LocalTime.parse(visitTime.substring(0, 5));
                        java.time.LocalTime now    = java.time.LocalTime.now();
                        if (now.isBefore(booked)) {
                            return "DENIED:Your entry window opens at " + visitTime.substring(0, 5)
                                + ". Please return at your scheduled time.";
                        }
                    } catch (Exception ex) {
                        // If time format is unexpected, allow entry rather than blocking on a parse error
                    }
                }

                // 4. Validate Park Capacity
                entities.Park park = getParkById(parkId);
                if (park != null && (park.getCurrentVisitors() + visitors > park.getMaxCapacity())) {
                    return "DENIED:Park is currently full. Wait for visitors to exit.";
                }

                // 5. Update the Order status to 'In Park'
                try (java.sql.PreparedStatement updateOrderStmt = conn.prepareStatement(updateOrderQuery)) {
                    updateOrderStmt.setInt(1, orderId);
                    updateOrderStmt.executeUpdate();
                }

                // 6. Update the live visitor count in the Park table
                try (java.sql.PreparedStatement updateParkStmt = conn.prepareStatement(updateParkQuery)) {
                    updateParkStmt.setInt(1, visitors);
                    updateParkStmt.setInt(2, parkId);
                    updateParkStmt.executeUpdate();
                }

                // 7. Return all receipt fields as pipe-delimited string for invoice generation
                // Format: APPROVED:{orderId}|{visitors}|{orderType}|{price}|{date}|{time}
                return "APPROVED:" + orderId + "|" + visitors + "|" + orderType
                    + "|" + String.format("%.0f", price)
                    + "|" + visitDate
                    + "|" + (visitTime != null ? visitTime.substring(0, Math.min(5, visitTime.length())) : "");
            }

        } catch (java.sql.SQLException e) {
            System.err.println("Database Error Processing Entry: " + e.getMessage());
            return "DENIED:Database error occurred while scanning ticket.";
        }
    }

    /**
     * Records a visitor exit by order ID: sets status to 'Completed',
     * stamps exit_timestamp, and decrements the park's current visitors.
     */
    public static String processParkExit(int orderId) {
        String selectQuery = "SELECT status, park_id, visitor_count FROM visit_order WHERE order_id = ?";
        String updateOrderQuery =
            "UPDATE visit_order SET status = 'Completed', exit_timestamp = NOW() WHERE order_id = ?";
        String updateParkQuery =
            "UPDATE park SET current_visitors = current_visitors - ? WHERE park_id = ?";

        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement sel = conn.prepareStatement(selectQuery)) {
                sel.setInt(1, orderId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        int parkId    = rs.getInt("park_id");
                        int visitors  = rs.getInt("visitor_count");

                        if (status.equals("In Park")) {
                            try (PreparedStatement upd = conn.prepareStatement(updateOrderQuery)) {
                                upd.setInt(1, orderId);
                                upd.executeUpdate();
                            }
                            try (PreparedStatement upd = conn.prepareStatement(updateParkQuery)) {
                                upd.setInt(1, visitors);
                                upd.setInt(2, parkId);
                                upd.executeUpdate();
                            }
                            conn.commit();
                            return "SUCCESS: " + visitors + " visitor(s) have safely exited the park.";
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

    /**
     * Manual exit: gate worker decrements park's current_visitors by a given count
     * without requiring a specific order ID. The count is clamped to the current
     * visitor count so it never goes negative.
     */
    public static String processManualExit(int parkId, int exitCount) {
        if (exitCount <= 0) return "Exit count must be a positive number.";

        String checkQuery  = "SELECT current_visitors FROM park WHERE park_id = ?";
        String updateQuery = "UPDATE park SET current_visitors = GREATEST(0, current_visitors - ?) WHERE park_id = ?";

        try (Connection conn = getInstance().getConnection()) {
            int current = 0;
            try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                ps.setInt(1, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) current = rs.getInt("current_visitors");
                }
            }
            if (current == 0) return "No visitors currently recorded in the park.";

            int actual = Math.min(exitCount, current);
            try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                ps.setInt(1, exitCount);
                ps.setInt(2, parkId);
                ps.executeUpdate();
            }
            return "SUCCESS: " + actual + " visitor(s) manually registered as exited.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database Error.";
        }
    }

    // =========================================================================
    // --- 4. PARK PARAMETERS ---
    // =========================================================================

    /** Fetches full park data for a given park ID, including the active promotional discount. */
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
                        rs.getInt("current_visitors"),
                        rs.getDouble("active_discount")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Submits a park parameter change request (capacity, gap, stay time).
     * Must be approved by the Department Manager before taking effect.
     */
    public static boolean submitParameterRequest(entities.Park park) {
        String query =
            "INSERT INTO parameter_request (park_id, new_max_capacity, new_casual_gap, new_estimated_stay_time, status) " +
            "VALUES (?, ?, ?, ?, 'Pending')";
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

    /** Returns all pending parameter change requests joined with park name. */
    public static ArrayList<entities.ParameterRequest> getPendingRequests() {
        ArrayList<entities.ParameterRequest> list = new ArrayList<>();
        String query =
            "SELECT pr.*, p.name AS park_name FROM parameter_request pr " +
            "JOIN park p ON pr.park_id = p.park_id WHERE pr.status = 'Pending' ORDER BY pr.request_id ASC";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new entities.ParameterRequest(
                    rs.getInt("request_id"),
                    rs.getInt("park_id"),
                    rs.getString("park_name"),
                    rs.getInt("new_max_capacity"),
                    rs.getInt("new_casual_gap"),
                    rs.getInt("new_estimated_stay_time"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Approves or denies a park parameter change request.
     * On approval, updates the park table with the new values.
     */
    public static boolean processParameterDecision(int requestId, String decision) {
        String updateReq = "UPDATE parameter_request SET status = ? WHERE request_id = ?";
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateReq)) {
                ps.setString(1, decision);
                ps.setInt(2, requestId);
                if (ps.executeUpdate() == 0) { conn.rollback(); return false; }
            }

            if ("Approved".equals(decision)) {
                int parkId = 0, newMax = 0, newGap = 0, newStay = 0;
                String getReq =
                    "SELECT park_id, new_max_capacity, new_casual_gap, new_estimated_stay_time " +
                    "FROM parameter_request WHERE request_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(getReq)) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            parkId  = rs.getInt("park_id");
                            newMax  = rs.getInt("new_max_capacity");
                            newGap  = rs.getInt("new_casual_gap");
                            newStay = rs.getInt("new_estimated_stay_time");
                        } else { conn.rollback(); return false; }
                    }
                }
                String updatePark =
                    "UPDATE park SET max_capacity = ?, casual_gap = ?, estimated_stay_time = ? WHERE park_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePark)) {
                    ps.setInt(1, newMax);
                    ps.setInt(2, newGap);
                    ps.setInt(3, newStay);
                    ps.setInt(4, parkId);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // --- 5. PROMOTIONAL DISCOUNTS ---
    // =========================================================================

    /**
     * Submits a promotional discount request from a Park Manager.
     * The request enters 'Pending' status until the Department Manager acts on it.
     */
    public static String submitPromotionRequest(entities.Promotion promo) {
        if (promo.getDiscountPercent() <= 0 || promo.getDiscountPercent() > 100) {
            return "Error: Discount must be between 1% and 100%.";
        }
        String query =
            "INSERT INTO park_promotion (park_id, park_name, discount_percent, status) VALUES (?, ?, ?, 'Pending')";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, promo.getParkId());
            ps.setString(2, promo.getParkName());
            ps.setDouble(3, promo.getDiscountPercent());
            ps.executeUpdate();
            return "SUCCESS: Promotion request for " + promo.getDiscountPercent() + "% submitted for approval.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during promotion submission.";
        }
    }

    /** Returns all pending promotional discount requests. */
    public static ArrayList<entities.Promotion> getPendingPromotions() {
        ArrayList<entities.Promotion> list = new ArrayList<>();
        String query =
            "SELECT * FROM park_promotion WHERE status = 'Pending' ORDER BY promotion_id ASC";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new entities.Promotion(
                    rs.getInt("promotion_id"),
                    rs.getInt("park_id"),
                    rs.getString("park_name"),
                    rs.getDouble("discount_percent"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Approves or denies a promotional discount request.
     * On approval, the park's active_discount is updated immediately.
     */
    public static boolean processPromotionDecision(int promotionId, String decision) {
        String updatePromo = "UPDATE park_promotion SET status = ? WHERE promotion_id = ?";
        try (Connection conn = getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updatePromo)) {
                ps.setString(1, decision);
                ps.setInt(2, promotionId);
                if (ps.executeUpdate() == 0) { conn.rollback(); return false; }
            }

            if ("Approved".equals(decision)) {
                int parkId = 0;
                double discountPct = 0;
                String getPromo =
                    "SELECT park_id, discount_percent FROM park_promotion WHERE promotion_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(getPromo)) {
                    ps.setInt(1, promotionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            parkId      = rs.getInt("park_id");
                            discountPct = rs.getDouble("discount_percent");
                        } else { conn.rollback(); return false; }
                    }
                }
                String updatePark = "UPDATE park SET active_discount = ? WHERE park_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePark)) {
                    ps.setDouble(1, discountPct);
                    ps.setInt(2, parkId);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Immediately cancels any active promotional discount on the given park.
     * Sets active_discount = 0 and marks any pending promotions for that park as Cancelled.
     * Called directly by the Department Manager — no Park Manager request required.
     */
    public static String cancelActivePromotion(int parkId) {
        String getParkName  = "SELECT name FROM park WHERE park_id = ?";
        String zeroDiscount = "UPDATE park SET active_discount = 0 WHERE park_id = ?";
        String cancelPending =
            "UPDATE park_promotion SET status = 'Cancelled' " +
            "WHERE park_id = ? AND status = 'Pending'";

        try (Connection conn = getInstance().getConnection()) {
            String parkName = "Park " + parkId;
            try (PreparedStatement ps = conn.prepareStatement(getParkName)) {
                ps.setInt(1, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) parkName = rs.getString("name");
                }
            }

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(zeroDiscount)) {
                ps.setInt(1, parkId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(cancelPending)) {
                ps.setInt(1, parkId);
                ps.executeUpdate();
            }
            conn.commit();
            return "SUCCESS: Active discount for " + parkName + " has been cancelled.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure while cancelling discount.";
        }
    }

    // =========================================================================
    // --- 6. SERVICE REPRESENTATIVE REGISTRATION ---
    // =========================================================================

    /**
     * Registers a new subscriber or tour guide.
     * Returns a SUCCESS string with the generated subscriber ID, or an error message.
     */
    public static String registerNewSubscriber(entities.Subscriber sub) {
        try (java.sql.Connection conn = getInstance().getConnection()) {

            String checkSub = "SELECT subscriber_id FROM subscriber WHERE visitor_id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(checkSub)) {
                ps.setString(1, sub.getVisitorId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return "Error: ID " + sub.getVisitorId() + " is already a registered subscriber.";
                }
            }

            String visType = sub.isGuide() ? "Guide" : "Subscriber";

            boolean visitorExists = false;
            String checkVis = "SELECT visitor_id FROM visitor WHERE visitor_id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(checkVis)) {
                ps.setString(1, sub.getVisitorId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) visitorExists = true;
                }
            }

            if (visitorExists) {
                String updateVis =
                    "UPDATE visitor SET first_name=?, last_name=?, email=?, phone=?, visitor_type=? WHERE visitor_id=?";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(updateVis)) {
                    ps.setString(1, sub.getFirstName());
                    ps.setString(2, sub.getLastName());
                    ps.setString(3, sub.getEmail());
                    ps.setString(4, sub.getPhone());
                    ps.setString(5, visType);
                    ps.setString(6, sub.getVisitorId());
                    ps.executeUpdate();
                }
            } else {
                String insertVis =
                    "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(insertVis)) {
                    ps.setString(1, sub.getVisitorId());
                    ps.setString(2, sub.getFirstName());
                    ps.setString(3, sub.getLastName());
                    ps.setString(4, sub.getEmail());
                    ps.setString(5, sub.getPhone());
                    ps.setString(6, visType);
                    ps.executeUpdate();
                }
            }

            String insertSub =
                "INSERT INTO subscriber (visitor_id, family_size, credit_card, is_guide) VALUES (?, ?, ?, ?)";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(insertSub, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sub.getVisitorId());
                ps.setInt(2, sub.getFamilySize());
                ps.setString(3, sub.getCreditCard());
                ps.setBoolean(4, sub.isGuide());
                ps.executeUpdate();

                try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newSubId = keys.getInt(1);
                        return "SUCCESS: " + visType + " registered! (Subscriber #" + newSubId + ")";
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

    /** Returns all orders for a given visitor ID. */
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

    /**
     * Confirms an order and resets notification_time.
     * Only transitions from 'Pending Confirm' or 'Waitlist Pending' are legal.
     * Returns false (no-op) for any other status — prevents re-confirming cancelled/completed orders.
     */
    public static boolean confirmOrder(int orderId) {
        // Fetch visit and visitor details for the response email before updating
        String fetchQ =
            "SELECT v.visit_date, v.visit_time, v.visitor_count, " +
            "vis.first_name, vis.email, vis.phone " +
            "FROM visit_order v JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
            "WHERE v.order_id = ?";
        String fName = null, email = null, phone = null;
        String visitDate = null, visitTime = null;
        int visitorCount = 0;
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(fetchQ)) {
            ps.setInt(1, orderId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    visitDate    = rs.getString("visit_date");
                    visitTime    = rs.getString("visit_time");
                    visitorCount = rs.getInt("visitor_count");
                    fName        = rs.getString("first_name");
                    email        = rs.getString("email");
                    phone        = rs.getString("phone");
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        String query =
            "UPDATE visit_order SET status = 'Confirmed', notification_time = NULL " +
            "WHERE order_id = ? AND status IN ('Pending Confirm', 'Waitlist Pending')";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, orderId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                String shortTime = visitTime != null
                    ? visitTime.substring(0, Math.min(5, visitTime.length())) : "";
                try {
                    if (email != null && email.contains("@")) {
                        String subject = "You're all set! — GoNature Order #" + orderId;
                        String htmlBody = buildConfirmResponseEmail(fName, orderId, visitDate, shortTime, visitorCount);
                        EmailSender.sendHtmlEmail(email, subject, htmlBody);
                    }
                } catch (Throwable ex) {
                    System.err.println("Confirm response email failed: " + ex.getMessage());
                }
                try {
                    SmsSender.sendSms(phone,
                        "GoNature: Visit confirmed! Order #" + orderId + " on " + visitDate
                        + " at " + shortTime + ". See you there!");
                } catch (Throwable ignored) {}
            }
            return updated;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Cancels an order and triggers waitlist promotion for the freed slot.
     *  Also sends the booker an Email + SMS cancellation notice per spec
     *  ("ונשלחת אליו הודעה על כך באימייל וב-SMS"). */
    public static boolean cancelOrder(int orderId) {
        String getDetails =
            "SELECT v.park_id, v.visit_date, v.visit_time, " +
            "       vis.first_name, vis.email, vis.phone " +
            "FROM visit_order v " +
            "JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
            "WHERE v.order_id = ?";
        // Guard: never cancel orders that are already terminal or currently In Park
        String cancel =
            "UPDATE visit_order SET status = 'Cancelled' " +
            "WHERE order_id = ? AND status NOT IN ('In Park', 'Completed', 'Cancelled')";

        int parkId = -1;
        String vDate = null, vTime = null;
        String vName = null, vEmail = null, vPhone = null;

        try (java.sql.Connection conn = getInstance().getConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(getDetails)) {
                ps.setInt(1, orderId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        parkId = rs.getInt("park_id");
                        vDate  = rs.getString("visit_date");
                        vTime  = rs.getString("visit_time");
                        vName  = rs.getString("first_name");
                        vEmail = rs.getString("email");
                        vPhone = rs.getString("phone");
                    }
                }
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement(cancel)) {
                ps.setInt(1, orderId);
                int rows = ps.executeUpdate();
                if (rows > 0 && parkId != -1) {

                    // --- Notify the booker (Email + SMS) per spec ---
                    try {
                        String shortT = (vTime != null)
                            ? vTime.substring(0, Math.min(5, vTime.length())) : "";
                        if (vEmail != null && vEmail.contains("@")) {
                            String subject = "Your GoNature booking has been cancelled — Order #" + orderId;
                            String htmlBody = buildCancelEmail(vName, orderId, vDate, shortT);
                            EmailSender.sendHtmlEmail(vEmail, subject, htmlBody);
                        }
                        SmsSender.sendSms(vPhone,
                            "GoNature: Booking #" + orderId + " for " + vDate +
                            " at " + vTime + " has been cancelled.");
                    } catch (Throwable notifyEx) {
                        System.err.println("Cancellation notification failed: "
                            + notifyEx.getMessage());
                    }

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
            int maxCapacity = 0, casualGap = 0, currentBooked = 0, estimatedStayTime = 4;
            String parkQ = "SELECT max_capacity, casual_gap, estimated_stay_time FROM park WHERE park_id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(parkQ)) {
                ps.setInt(1, parkId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity = rs.getInt("max_capacity");
                        casualGap   = rs.getInt("casual_gap");
                        int stay    = rs.getInt("estimated_stay_time");
                        if (stay > 0) estimatedStayTime = stay;
                    }
                }
            }

            // Stay-time-aware overlap, matching processNewOrder's logic.
            String sumQ =
                "SELECT COALESCE(SUM(visitor_count), 0) AS total_visitors FROM visit_order " +
                "WHERE park_id = ? AND visit_date = ? " +
                "AND HOUR(visit_time) > HOUR(?) - ? " +
                "AND HOUR(visit_time) < HOUR(?) + ? " +
                "AND status IN ('Booked', 'Confirmed', 'In Park', 'Pending Confirm', 'Waitlist Pending')";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sumQ)) {
                ps.setInt(1, parkId);
                ps.setString(2, date);
                ps.setString(3, time);
                ps.setInt(4, estimatedStayTime);
                ps.setString(5, time);
                ps.setInt(6, estimatedStayTime);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) currentBooked = rs.getInt("total_visitors");
                }
            }

            int availableSpace = (maxCapacity - casualGap) - currentBooked;
            if (availableSpace <= 0) return;

            String waitlistQ =
                "SELECT v.order_id, v.visitor_count, vis.first_name, vis.email, vis.phone " +
                "FROM visit_order v JOIN visitor vis ON v.visitor_id = vis.visitor_id " +
                "WHERE v.park_id = ? AND v.visit_date = ? AND v.visit_time = ? AND v.status = 'Waitlisted' " +
                "ORDER BY v.order_id ASC";

            try (java.sql.PreparedStatement ps = conn.prepareStatement(waitlistQ)) {
                ps.setInt(1, parkId); ps.setString(2, date); ps.setString(3, time);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int waitId     = rs.getInt("order_id");
                        int groupSize  = rs.getInt("visitor_count");
                        String email   = rs.getString("email");
                        String phone   = rs.getString("phone");
                        String fName   = rs.getString("first_name");

                        if (groupSize <= availableSpace) {
                            String promote =
                                "UPDATE visit_order SET status = 'Waitlist Pending', notification_time = NOW() WHERE order_id = ?";
                            try (java.sql.PreparedStatement ps2 = conn.prepareStatement(promote)) {
                                ps2.setInt(1, waitId);
                                ps2.executeUpdate();

                                String confirmUrl = serverBaseUrl + "/confirm?id=" + waitId;
                                String cancelUrl  = serverBaseUrl + "/cancel?id="  + waitId;
                                System.out.println("⚠ SYSTEM ALERT: Order #" + waitId + " promoted from waitlist.");
                                System.out.println("  ↳ CONFIRM: " + confirmUrl);
                                System.out.println("  ↳ CANCEL:  " + cancelUrl);

                                if (email != null && email.contains("@")) {
                                    String subject = "🎉 A spot opened! Confirm within 1 hour — GoNature Order #" + waitId;
                                    String htmlBody = buildWaitlistEmail(fName, waitId, date, time, groupSize, confirmUrl, cancelUrl);
                                    EmailSender.sendHtmlEmail(email, subject, htmlBody);
                                }
                                SmsSender.sendSms(phone,
                                    "GoNature: Spot available for Order #" + waitId + " on " + date +
                                    " at " + time + ". Confirm within 1 hour: " + confirmUrl);

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

    /**
     * Generates a monthly visitor and income report for a specific park.
     * Only includes completed and in-park visits.
     */
    public static entities.ReportData generateMonthlyReport(int parkId, String month, String year) {
        entities.ReportData report = new entities.ReportData(parkId, month, year);
        String query =
            "SELECT order_type, SUM(visitor_count) AS total_visitors, SUM(price) AS total_income " +
            "FROM visit_order " +
            "WHERE park_id = ? AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
            "AND status IN ('Completed', 'In Park') GROUP BY order_type";

        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, parkId);
            ps.setInt(2, Integer.parseInt(month));
            ps.setInt(3, Integer.parseInt(year));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type     = rs.getString("order_type");
                    int visitors    = rs.getInt("total_visitors");
                    double income   = rs.getDouble("total_income");
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

    /** Returns true if the given visitor ID belongs to a registered tour guide. */
    public static boolean isRegisteredGuide(String visitorId) {
        String query = "SELECT is_guide FROM subscriber WHERE visitor_id = ? AND is_guide = true";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Returns true if the given visitor ID is a registered subscriber (any type). */
    public static boolean isSubscriber(String visitorId) {
        String query = "SELECT visitor_id FROM subscriber WHERE visitor_id = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
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

    /**
     * Processes a walk-in (unplanned) visit.
     * Applies walk-in pricing with any active promotional discount.
     * The entry_timestamp is stamped immediately since they enter on the spot.
     */
    public static entities.VisitOrder processWalkIn(int parkId, int visitorCount, String orderType, String subscriberId) {
        try (Connection conn = getInstance().getConnection()) {
            int maxCapacity = 0, currentVisitors = 0;
            double activeDiscount = 0;

            String parkQ = "SELECT max_capacity, current_visitors, active_discount FROM park WHERE park_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(parkQ)) {
                ps.setInt(1, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity    = rs.getInt("max_capacity");
                        currentVisitors = rs.getInt("current_visitors");
                        activeDiscount = rs.getDouble("active_discount");
                    }
                }
            }

            if (maxCapacity - currentVisitors < visitorCount) return null;

            // Walk-in pricing: full price for personal/family; 10% off for groups (guide pays)
            int basePrice = 100;
            double price;
            if (orderType.equalsIgnoreCase("Group")) {
                price = visitorCount * (basePrice * 0.90);
            } else {
                price = visitorCount * basePrice;
            }

            // Apply park-level promotional discount
            if (activeDiscount > 0) {
                price = price * (1.0 - activeDiscount / 100.0);
            }

            // Subscriber gets additional 10% off; use their real visitor_id for tracking
            String walkInId;
            if (subscriberId != null && !subscriberId.isEmpty()) {
                String subQ = "SELECT visitor_id FROM subscriber WHERE visitor_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(subQ)) {
                    ps.setString(1, subscriberId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            price *= 0.90;
                            walkInId = subscriberId;
                        } else {
                            walkInId = "WI" + (System.currentTimeMillis() % 1000000);
                        }
                    }
                }
            } else {
                walkInId = "WI" + (System.currentTimeMillis() % 1000000);
            }

            String today   = java.time.LocalDate.now().toString();
            String nowTime = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ":00";

            String ensureVisitor =
                "INSERT INTO visitor (visitor_id, first_name, last_name, email, phone, visitor_type) " +
                "VALUES (?, 'Walk-in', 'Visitor', 'Not Provided', 'Not Provided', 'Regular') " +
                "ON DUPLICATE KEY UPDATE visitor_id=visitor_id";
            try (PreparedStatement ps = conn.prepareStatement(ensureVisitor)) {
                ps.setString(1, walkInId);
                ps.executeUpdate();
            }

            String insertQ =
                "INSERT INTO visit_order (park_id, visitor_id, visit_date, visit_time, visitor_count, " +
                "order_type, status, price, entry_timestamp) VALUES (?, ?, ?, ?, ?, ?, 'In Park', ?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(insertQ, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, parkId);
                ps.setString(2, walkInId);
                ps.setString(3, today);
                ps.setString(4, nowTime);
                ps.setInt(5, visitorCount);
                ps.setString(6, orderType);
                ps.setDouble(7, price);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        String updatePark =
                            "UPDATE park SET current_visitors = current_visitors + ? WHERE park_id = ?";
                        try (PreparedStatement upd = conn.prepareStatement(updatePark)) {
                            upd.setInt(1, visitorCount);
                            upd.setInt(2, parkId);
                            upd.executeUpdate();
                        }
                        return new entities.VisitOrder(newId, parkId, walkInId, today, nowTime,
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

    /** Returns all parks with current parameters, including active promotional discount. */
    public static ArrayList<entities.Park> getAllParks() {
        ArrayList<entities.Park> parks = new ArrayList<>();
        String query = "SELECT * FROM park";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                parks.add(new entities.Park(
                    rs.getInt("park_id"), rs.getString("name"),
                    rs.getInt("max_capacity"), rs.getInt("casual_gap"),
                    rs.getInt("estimated_stay_time"), rs.getInt("current_visitors"),
                    rs.getDouble("active_discount")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parks;
    }

    /**
     * Retrieves a previously submitted monthly report from the monthly_reports table.
     * Returns null if no report was submitted for the given park/period.
     */
    public static entities.ReportData getDeptMonthlyReport(int parkId, String month, String year) {
        String query =
            "SELECT * FROM monthly_reports WHERE park_id = ? AND report_month = ? AND report_year = ?";
        try (Connection conn = getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, parkId);
            ps.setString(2, month);
            ps.setString(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entities.ReportData report = new entities.ReportData(parkId, month, year);
                    report.addVisitorData("Solo",       rs.getInt("solo_visitors"));
                    report.addVisitorData("Family",     rs.getInt("family_visitors"));
                    report.addVisitorData("Group",      rs.getInt("group_visitors"));
                    report.addVisitorData("Subscriber", rs.getInt("subscriber_visitors"));
                    report.addIncomeData("Income",      rs.getDouble("total_income"));
                    return report;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generates a cancellations report showing:
     * - Orders explicitly cancelled (by type)
     * - No-shows: orders whose visit time has passed but were never cancelled or entered
     * - Daily distribution of cancellations (count per day of week)
     *
     * @param parkId  Filter to a specific park (0 = all parks in the area)
     * @param month   Month number as string
     * @param year    Year as string
     */
    public static entities.ReportData getCancellationsReport(int parkId, String month, String year) {
        entities.ReportData report = new entities.ReportData(parkId, month, year);
        int monthInt = Integer.parseInt(month);
        int yearInt  = Integer.parseInt(year);

        String parkFilter = (parkId > 0) ? " AND park_id = ? " : " ";

        // 1. Explicitly cancelled orders
        String cancelQ =
            "SELECT order_type, COUNT(*) AS cnt FROM visit_order " +
            "WHERE status = 'Cancelled' AND MONTH(visit_date) = ? AND YEAR(visit_date) = ?" +
            parkFilter + "GROUP BY order_type";

        // 2. No-shows: visit time passed but still in an 'active' status
        String noShowQ =
            "SELECT order_type, COUNT(*) AS cnt FROM visit_order " +
            "WHERE status IN ('Booked', 'Confirmed', 'Pending Confirm', 'Waitlisted', 'Waitlist Pending') " +
            "AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
            "AND TIMESTAMP(visit_date, visit_time) < NOW()" +
            parkFilter + "GROUP BY order_type";

        // 3. Daily distribution: cancellations per day of week for averages
        String dailyQ =
            "SELECT DAYNAME(visit_date) AS day_name, COUNT(*) AS cnt FROM visit_order " +
            "WHERE status = 'Cancelled' AND MONTH(visit_date) = ? AND YEAR(visit_date) = ?" +
            parkFilter + "GROUP BY day_name";

        try (Connection conn = getInstance().getConnection()) {

            // Cancelled
            try (PreparedStatement ps = conn.prepareStatement(cancelQ)) {
                ps.setInt(1, monthInt); ps.setInt(2, yearInt);
                if (parkId > 0) ps.setInt(3, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        report.addVisitorData(rs.getString("order_type") + " Cancelled", rs.getInt("cnt"));
                }
            }

            // No-shows
            try (PreparedStatement ps = conn.prepareStatement(noShowQ)) {
                ps.setInt(1, monthInt); ps.setInt(2, yearInt);
                if (parkId > 0) ps.setInt(3, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        report.addVisitorData(rs.getString("order_type") + " No-Show", rs.getInt("cnt"));
                }
            }

            // Daily distribution stored in incomeBreakdown (day → count as double)
            try (PreparedStatement ps = conn.prepareStatement(dailyQ)) {
                ps.setInt(1, monthInt); ps.setInt(2, yearInt);
                if (parkId > 0) ps.setInt(3, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        report.addIncomeData(rs.getString("day_name"), (double) rs.getInt("cnt"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }

    /**
     * Generates a visit-detail report showing visitor count and average stay time
     * (in hours) per order type for the given park and period.
     * Uses entry_timestamp and exit_timestamp stamped at the gate.
     * Visitor type breakdown is stored in visitorBreakdown;
     * average stay time per type is stored in incomeBreakdown (value = hours).
     */
    public static entities.ReportData getVisitReport(int parkId, String month, String year) {
        entities.ReportData report = new entities.ReportData(parkId, month, year);
        int monthInt = Integer.parseInt(month);
        int yearInt  = Integer.parseInt(year);

        // Visitor counts and avg stay (only completed visits that have both timestamps)
        String stayQ =
            "SELECT order_type, COUNT(*) AS cnt, " +
            "AVG(TIMESTAMPDIFF(MINUTE, entry_timestamp, exit_timestamp)) AS avg_min " +
            "FROM visit_order " +
            "WHERE park_id = ? AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
            "AND status = 'Completed' AND entry_timestamp IS NOT NULL AND exit_timestamp IS NOT NULL " +
            "GROUP BY order_type";

        // Entry-hour distribution across all visits that have an entry timestamp
        String hourQ =
            "SELECT HOUR(entry_timestamp) AS hr, COUNT(*) AS cnt " +
            "FROM visit_order " +
            "WHERE park_id = ? AND MONTH(visit_date) = ? AND YEAR(visit_date) = ? " +
            "AND entry_timestamp IS NOT NULL " +
            "GROUP BY hr ORDER BY hr";

        try (Connection conn = getInstance().getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(stayQ)) {
                ps.setInt(1, parkId); ps.setInt(2, monthInt); ps.setInt(3, yearInt);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type  = rs.getString("order_type");
                        int cnt      = rs.getInt("cnt");
                        double avgMin = rs.getDouble("avg_min");
                        report.addVisitorData(type, cnt);
                        // Avg stay in hours stored in incomeBreakdown for chart display
                        report.addIncomeData(type + " Avg Stay (hrs)", avgMin / 60.0);
                    }
                }
            }

            // Entry-hour distribution stored as visitor data keyed by "HH:00"
            try (PreparedStatement ps = conn.prepareStatement(hourQ)) {
                ps.setInt(1, parkId); ps.setInt(2, monthInt); ps.setInt(3, yearInt);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int hr  = rs.getInt("hr");
                        int cnt = rs.getInt("cnt");
                        // Use addIncomeData so it doesn't inflate totalVisitors
                        report.addIncomeData(String.format("%02d:00", hr) + " entries", (double) cnt);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return report;
    }

    // =========================================================================
    // --- 12. AVAILABLE SLOTS ---
    // =========================================================================

    /**
     * Returns a list of time slots with available capacity for a given park and date.
     * Used to suggest alternative dates when a booking is rejected or waitlisted.
     */
    public static ArrayList<String> getAvailableSlots(int parkId, String date) {
        ArrayList<String> slots = new ArrayList<>();
        String[] allSlots = {"08:00:00","09:00:00","10:00:00","11:00:00","12:00:00",
                             "13:00:00","14:00:00","15:00:00","16:00:00","17:00:00"};
        try (Connection conn = getInstance().getConnection()) {
            int maxCapacity = 0, casualGap = 0, estimatedStayTime = 4;
            String parkQ = "SELECT max_capacity, casual_gap, estimated_stay_time FROM park WHERE park_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(parkQ)) {
                ps.setInt(1, parkId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity = rs.getInt("max_capacity");
                        casualGap   = rs.getInt("casual_gap");
                        int stay    = rs.getInt("estimated_stay_time");
                        if (stay > 0) estimatedStayTime = stay;
                    }
                }
            }
            int available = maxCapacity - casualGap;
            // Stay-time-aware overlap, matching processNewOrder.
            String sumQ =
                "SELECT COALESCE(SUM(visitor_count),0) AS booked FROM visit_order " +
                "WHERE park_id=? AND visit_date=? " +
                "AND HOUR(visit_time) > HOUR(?) - ? " +
                "AND HOUR(visit_time) < HOUR(?) + ? " +
                "AND status IN ('Booked','Confirmed','In Park','Pending Confirm','Waitlist Pending')";
            for (String slot : allSlots) {
                try (PreparedStatement ps = conn.prepareStatement(sumQ)) {
                    ps.setInt(1, parkId);
                    ps.setString(2, date);
                    ps.setString(3, slot);
                    ps.setInt(4, estimatedStayTime);
                    ps.setString(5, slot);
                    ps.setInt(6, estimatedStayTime);
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

    // =========================================================================
    // --- 13. GUIDE VERIFICATION ---
    // =========================================================================

    /** Saves a monthly report submitted by a Park Manager to the monthly_reports table. */
    public static String saveMonthlyReport(entities.ReportData report) {
        int solo       = report.getVisitorBreakdown().getOrDefault("Solo", 0);
        int family     = report.getVisitorBreakdown().getOrDefault("Family", 0);
        int group      = report.getVisitorBreakdown().getOrDefault("Group", 0);
        int subscriber = report.getVisitorBreakdown().getOrDefault("Subscriber", 0);

        String query =
            "INSERT INTO monthly_reports (park_id, report_month, report_year, total_visitors, total_income, " +
            "solo_visitors, family_visitors, group_visitors, subscriber_visitors) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_visitors=VALUES(total_visitors), total_income=VALUES(total_income), " +
            "solo_visitors=VALUES(solo_visitors), family_visitors=VALUES(family_visitors), " +
            "group_visitors=VALUES(group_visitors), subscriber_visitors=VALUES(subscriber_visitors)";

        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, report.getParkId());
            ps.setString(2, report.getMonth());
            ps.setString(3, report.getYear());
            ps.setInt(4, report.getTotalVisitors());
            ps.setDouble(5, report.getTotalIncome());
            ps.setInt(6, solo);
            ps.setInt(7, family);
            ps.setInt(8, group);
            ps.setInt(9, subscriber);
            ps.executeUpdate();
            return "SUCCESS: Report successfully submitted to the Department Manager.";
        } catch (java.sql.SQLException e) {
            System.err.println("Database Error Saving Report: " + e.getMessage());
            return "ERROR: Could not save the report to the database.";
        }
    }

    /** Returns the park_id for a given order. Used by broadcast helpers in EchoServer. */
    public static int getOrderParkId(int orderId) {
        String query = "SELECT park_id FROM visit_order WHERE order_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, orderId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("park_id");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // =========================================================================
    // --- 14. USAGE REPORT ---
    // =========================================================================

    /**
     * Returns a list of days in the given month where the park was below full capacity.
     * Each row: [date, daily_visitors, max_capacity, available_slots, utilization_%].
     */
    public static ArrayList<String[]> getUsageReport(int parkId, int month, int year) {
        ArrayList<String[]> rows = new ArrayList<>();
        String query =
            "SELECT vo.visit_date, COALESCE(SUM(vo.visitor_count), 0) AS daily_total, p.max_capacity " +
            "FROM visit_order vo " +
            "JOIN park p ON vo.park_id = p.park_id " +
            "WHERE vo.park_id = ? AND MONTH(vo.visit_date) = ? AND YEAR(vo.visit_date) = ? " +
            "  AND vo.status IN ('Booked','Confirmed','In Park','Completed','Pending Confirm','Waitlist Pending') " +
            "GROUP BY vo.visit_date, p.max_capacity " +
            "ORDER BY vo.visit_date";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, parkId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String date     = rs.getString("visit_date");
                    int visitors    = rs.getInt("daily_total");
                    int maxCap      = rs.getInt("max_capacity");
                    int available   = maxCap - visitors;
                    double pct      = maxCap > 0 ? (visitors * 100.0 / maxCap) : 0;
                    String fullFlag = (available <= 0) ? "FULL" : "Available";
                    rows.add(new String[]{ date, String.valueOf(visitors), String.valueOf(maxCap),
                                          String.valueOf(Math.max(0, available)), String.format("%.1f", pct), fullFlag });
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // =========================================================================
    // --- 15. SUBSCRIBER FAMILY SIZE CHECK ---
    // =========================================================================

    /**
     * Returns the family_size for a subscriber, or -1 if the visitor is not a subscriber.
     * Used to enforce that personal/family bookings don't exceed the covered member count.
     */
    public static int getSubscriberFamilySize(String visitorId) {
        String query = "SELECT family_size FROM subscriber WHERE visitor_id = ? AND is_guide = false";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, visitorId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("family_size");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching family size: " + e.getMessage());
        }
        return -1;
    }

    // =========================================================================
    // --- 16. SUBSCRIBER LOOKUP & UPDATE (Scenario 1 & 2) ---
    // =========================================================================

    /** Fetches full subscriber info by visitor_id for the Service Rep view. */
    public static entities.Subscriber getSubscriberById(String visitorId) {
        String query =
            "SELECT v.visitor_id, v.first_name, v.last_name, v.email, v.phone, " +
            "s.family_size, s.credit_card, s.is_guide " +
            "FROM visitor v JOIN subscriber s ON v.visitor_id = s.visitor_id " +
            "WHERE v.visitor_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, visitorId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new entities.Subscriber(
                        rs.getString("visitor_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("family_size"),
                        rs.getString("credit_card"),
                        rs.getBoolean("is_guide")
                    );
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching subscriber: " + e.getMessage());
        }
        return null;
    }

    /** Updates personal info for an existing subscriber (used by Service Rep and subscriber self-service). */
    public static String updateSubscriberInfo(entities.Subscriber sub) {
        String updateVisitor =
            "UPDATE visitor SET first_name=?, last_name=?, email=?, phone=? WHERE visitor_id=?";
        String updateSubscriber =
            "UPDATE subscriber SET family_size=?, credit_card=? WHERE visitor_id=?";
        try (java.sql.Connection conn = getInstance().getConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(updateVisitor)) {
                ps.setString(1, sub.getFirstName());
                ps.setString(2, sub.getLastName());
                ps.setString(3, sub.getEmail());
                ps.setString(4, sub.getPhone());
                ps.setString(5, sub.getVisitorId());
                if (ps.executeUpdate() == 0) return "ERROR: Subscriber ID not found.";
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement(updateSubscriber)) {
                ps.setInt(1, sub.getFamilySize());
                ps.setString(2, sub.getCreditCard());
                ps.setString(3, sub.getVisitorId());
                ps.executeUpdate();
            }
            return "SUCCESS: Subscriber info updated.";
        } catch (java.sql.SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // =========================================================================
    // --- 17. ORDER LOOKUP & EDIT (Guest Portal edit-order and HTTP confirm) ---
    // =========================================================================

    /** Returns a single VisitOrder by its primary key, or null if not found. */
    public static entities.VisitOrder getOrderById(int orderId) {
        String query = "SELECT order_id, park_id, visitor_id, visit_date, visit_time, " +
                       "visitor_count, order_type, status, price FROM visit_order WHERE order_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, orderId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new entities.VisitOrder(
                        rs.getInt("order_id"), rs.getInt("park_id"),
                        rs.getString("visitor_id"),
                        rs.getString("visit_date"), rs.getString("visit_time"),
                        rs.getInt("visitor_count"), rs.getString("order_type"),
                        rs.getString("status"), rs.getDouble("price")
                    );
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("getOrderById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Applies guest edits (date/time/visitorCount) to an order that is in 'Confirmed'
     * or 'Waitlisted' status. Recalculates price and re-evaluates capacity.
     *
     * Returns a response string: "SUCCESS:newStatus:newPrice" or "ERROR:reason".
     */
    public static String updateOrderDetails(entities.VisitOrder updated) {
        String checkQ = "SELECT park_id, visitor_id, order_type, status FROM visit_order WHERE order_id = ?";
        try (java.sql.Connection conn = getInstance().getConnection()) {
            int parkId; String visitorId, orderType, currentStatus;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(checkQ)) {
                ps.setInt(1, updated.getOrderId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return "ERROR:Order not found.";
                    parkId        = rs.getInt("park_id");
                    visitorId     = rs.getString("visitor_id");
                    orderType     = rs.getString("order_type");
                    currentStatus = rs.getString("status");
                }
            }
            if (!currentStatus.equals("Booked")) {
                return "ERROR:Only 'Booked' orders can be edited. Once your attendance is confirmed or the 24h reminder is sent, editing is no longer available.";
            }

            // Park capacity parameters
            int maxCapacity = 200, casualGap = 20, estimatedStayTime = 4;
            double activeDiscount = 0;
            String parkQ = "SELECT max_capacity, casual_gap, estimated_stay_time, active_discount FROM park WHERE park_id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(parkQ)) {
                ps.setInt(1, parkId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        maxCapacity      = rs.getInt("max_capacity");
                        casualGap        = rs.getInt("casual_gap");
                        int stayH        = rs.getInt("estimated_stay_time");
                        if (stayH > 0) estimatedStayTime = stayH;
                        activeDiscount   = rs.getDouble("active_discount");
                    }
                }
            }

            // Count competing bookings at the new slot, excluding this order itself
            String sumQ =
                "SELECT COALESCE(SUM(visitor_count),0) AS total FROM visit_order " +
                "WHERE park_id=? AND visit_date=? " +
                "AND HOUR(visit_time) > HOUR(?)-? AND HOUR(visit_time) < HOUR(?)+? " +
                "AND status IN ('Booked','Confirmed','In Park','Pending Confirm','Waitlist Pending') " +
                "AND order_id != ?";
            int booked = 0;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sumQ)) {
                ps.setInt(1, parkId);
                ps.setString(2, updated.getVisitDate());
                ps.setString(3, updated.getVisitTime()); ps.setInt(4, estimatedStayTime);
                ps.setString(5, updated.getVisitTime()); ps.setInt(6, estimatedStayTime);
                ps.setInt(7, updated.getOrderId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) booked = rs.getInt("total");
                }
            }

            int availablePreorder = maxCapacity - casualGap;
            String newStatus = ((booked + updated.getVisitorCount()) > availablePreorder)
                    ? "Waitlisted" : "Booked";

            // Recalculate price
            boolean isSubscriber = false, isGuide = false;
            String subQ = "SELECT is_guide FROM subscriber WHERE visitor_id=?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(subQ)) {
                ps.setString(1, visitorId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { isSubscriber = true; isGuide = rs.getBoolean("is_guide"); }
                }
            }

            double basePrice = 100.0, price;
            if (isGuide || orderType.equalsIgnoreCase("Group")) {
                int paying = Math.max(0, updated.getVisitorCount() - 1);
                price = paying * (basePrice * 0.75 * 0.88);
                if (isSubscriber) price *= 0.90;
            } else if (isSubscriber) {
                price = updated.getVisitorCount() * (basePrice * 0.85 * 0.90);
            } else {
                price = updated.getVisitorCount() * (basePrice * 0.85);
            }
            if (activeDiscount > 0) price *= (1.0 - activeDiscount / 100.0);

            String updateQ =
                "UPDATE visit_order SET visit_date=?, visit_time=?, visitor_count=?, price=?, " +
                "status=?, notification_time=NULL WHERE order_id=?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(updateQ)) {
                ps.setString(1, updated.getVisitDate());
                ps.setString(2, updated.getVisitTime());
                ps.setInt(3, updated.getVisitorCount());
                ps.setDouble(4, price);
                ps.setString(5, newStatus);
                ps.setInt(6, updated.getOrderId());
                ps.executeUpdate();
            }
            return "SUCCESS:" + newStatus + ":" + String.format("%.2f", price);

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return "ERROR:Database error: " + e.getMessage();
        }
    }

    // =========================================================================
    // --- 18. HTML EMAIL BUILDERS (24h reminder & waitlist promotion) ---
    // =========================================================================

    /** Builds the styled booking-confirmation HTML email body sent immediately after booking. */
    public static String buildBookingConfirmEmail(int orderId, String date, String time,
                                                   int visitors, double price, String cancelUrl) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — Booking Confirmed</title>"
            + "<style>"
            + "body{margin:0;padding:40px 0;background:#edf7ee;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".w{max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
            + "border:1px solid rgba(30,160,90,0.20);overflow:hidden;"
            + "box-shadow:0 6px 24px rgba(0,60,30,0.10);}"
            + ".hd{background:linear-gradient(135deg,#1A9E5A,#2DC975);padding:32px;text-align:center;}"
            + ".hd h1{color:#fff;margin:0;font-size:24px;font-weight:700;letter-spacing:.5px;}"
            + ".hd p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:14px;}"
            + ".bd{padding:32px;}"
            + ".greet{color:#0D2118;font-size:15px;margin-bottom:16px;font-weight:600;}"
            + ".note{color:#3D6B52;font-size:13px;line-height:1.7;margin-bottom:22px;}"
            + ".box{background:#f0fdf4;border-radius:10px;padding:20px;margin:20px 0;"
            + "border:1px solid rgba(30,160,90,0.18);}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;"
            + "border-bottom:1px solid rgba(30,160,90,0.12);}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#5a8a6a;font-size:13px;}.val{color:#0D2118;font-size:13px;font-weight:700;}"
            + ".qr{background:#f0fdf4;border:2px solid #1A9E5A;border-radius:10px;padding:22px;"
            + "text-align:center;margin:22px 0;}"
            + ".qr-code{font-size:44px;font-weight:900;color:#1A9E5A;letter-spacing:6px;}"
            + ".qr-label{color:#5a8a6a;font-size:11px;margin-top:8px;font-weight:600;letter-spacing:1px;}"
            + "a.btn-x{display:block;text-align:center;color:#5a8a6a!important;text-decoration:none;"
            + "font-size:13px;padding:10px;margin-top:10px;}"
            + ".ft{background:#f0fdf4;border-top:1px solid rgba(30,160,90,0.12);padding:18px;"
            + "text-align:center;color:#8aaa9a;font-size:11px;}"
            + "</style></head><body>"
            + "<div class='w'>"
            + "<div class='hd'><h1>🌿 GoNature</h1><p>Your upcoming visit</p></div>"
            + "<div class='bd'>"
            + "<p class='greet'>Thanks for booking with GoNature!</p>"
            + "<p class='note'>Here are the details for your upcoming visit. Present your Order ID at the park entrance when you arrive. "
            + "A reminder will be sent <strong>24 hours before your visit</strong> — you'll need to confirm within "
            + "<strong style='color:#d97706'>2 hours</strong> of that reminder to keep your spot.</p>"
            + "<div class='box'>"
            + "<div class='row'><span class='lbl'>Order #</span><span class='val'>" + orderId + "</span></div>"
            + "<div class='row'><span class='lbl'>Date</span><span class='val'>" + date + "</span></div>"
            + "<div class='row'><span class='lbl'>Time</span><span class='val'>" + time + "</span></div>"
            + "<div class='row'><span class='lbl'>Visitors</span><span class='val'>" + visitors + "</span></div>"
            + "<div class='row'><span class='lbl'>Total Price</span><span class='val'>₪" + String.format("%.0f", price) + "</span></div>"
            + "</div>"
            + "<div class='qr'>"
            + "<div class='qr-code'>#" + orderId + "</div>"
            + "<div class='qr-label'>PRESENT AT PARK ENTRANCE</div>"
            + "</div>"
            + "<a href='" + cancelUrl + "' class='btn-x'>Something came up? Cancel this booking</a>"
            + "</div>"
            + "<div class='ft'>GoNature Parks Management System &nbsp;·&nbsp; Automated message &nbsp;·&nbsp; Do not reply</div>"
            + "</div></body></html>";
    }

    /** Builds the styled 24-hour reminder HTML email body. */
    public static String buildReminderEmail(String firstName, int orderId, String date,
                                            String time, int visitors,
                                            String confirmUrl, String cancelUrl) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — Confirm Your Visit</title>"
            + "<style>"
            + "body{margin:0;padding:40px 0;background:#edf7ee;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".w{max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
            + "border:1px solid rgba(30,160,90,0.20);overflow:hidden;"
            + "box-shadow:0 6px 24px rgba(0,60,30,0.10);}"
            + ".hd{background:linear-gradient(135deg,#1A9E5A,#2DC975);padding:32px;text-align:center;}"
            + ".hd h1{color:#fff;margin:0;font-size:24px;font-weight:700;letter-spacing:.5px;}"
            + ".hd p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:14px;}"
            + ".bd{padding:32px;}"
            + ".greet{color:#0D2118;font-size:15px;margin-bottom:16px;}"
            + ".note{color:#3D6B52;font-size:13px;line-height:1.7;margin-bottom:22px;}"
            + ".box{background:#f0fdf4;border-radius:10px;padding:20px;margin:20px 0;"
            + "border:1px solid rgba(30,160,90,0.18);}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;"
            + "border-bottom:1px solid rgba(30,160,90,0.12);}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#5a8a6a;font-size:13px;}.val{color:#0D2118;font-size:13px;font-weight:700;}"
            + ".warn{background:#fffbeb;border:1px solid #f59e0b;border-radius:8px;padding:14px;"
            + "color:#92400e;font-size:13px;text-align:center;margin:20px 0;}"
            + "a.btn-c{display:block;background:#1A9E5A;color:#fff!important;text-decoration:none;"
            + "text-align:center;padding:16px 30px;border-radius:10px;font-size:16px;font-weight:700;"
            + "margin:22px 0 12px;letter-spacing:.3px;}"
            + "a.btn-c:hover{background:#20B868;}"
            + "a.btn-x{display:block;text-align:center;color:#5a8a6a!important;text-decoration:none;"
            + "font-size:13px;padding:8px;}"
            + ".ft{background:#f0fdf4;border-top:1px solid rgba(30,160,90,0.12);padding:18px;"
            + "text-align:center;color:#8aaa9a;font-size:11px;}"
            + "</style></head><body>"
            + "<div class='w'>"
            + "<div class='hd'><h1>🌿 GoNature</h1><p>Your visit is tomorrow — action required</p></div>"
            + "<div class='bd'>"
            + "<p class='greet'>Hello <strong style='color:#0D2118'>" + firstName + "</strong>,</p>"
            + "<p class='note'>This is your 24-hour reminder for tomorrow's visit. "
            + "Please confirm your attendance within <strong style='color:#d97706'>2 hours</strong> "
            + "or your booking will be automatically cancelled.</p>"
            + "<div class='box'>"
            + "<div class='row'><span class='lbl'>Order #</span><span class='val'>" + orderId + "</span></div>"
            + "<div class='row'><span class='lbl'>Date</span><span class='val'>" + date + "</span></div>"
            + "<div class='row'><span class='lbl'>Time</span><span class='val'>" + time + "</span></div>"
            + "<div class='row'><span class='lbl'>Visitors</span><span class='val'>" + visitors + "</span></div>"
            + "</div>"
            + "<div class='warn'>⏰ You must confirm within <strong>2 hours</strong> of receiving this email.</div>"
            + "<a href='" + confirmUrl + "' class='btn-c'>✅ Confirm My Visit</a>"
            + "<a href='" + cancelUrl  + "' class='btn-x'>I can't make it — Cancel my booking</a>"
            + "</div>"
            + "<div class='ft'>GoNature Parks Management System &nbsp;·&nbsp; Automated message &nbsp;·&nbsp; Do not reply</div>"
            + "</div></body></html>";
    }

    /** Response email sent when visitor clicks Confirm in the 24h reminder. */
    public static String buildConfirmResponseEmail(String firstName, int orderId, String date,
                                                    String time, int visitors) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — You're All Set!</title>"
            + "<style>"
            + "body{margin:0;padding:40px 0;background:#edf7ee;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".w{max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
            + "border:1px solid rgba(30,160,90,0.20);overflow:hidden;"
            + "box-shadow:0 6px 24px rgba(0,60,30,0.10);}"
            + ".hd{background:linear-gradient(135deg,#1A9E5A,#2DC975);padding:32px;text-align:center;}"
            + ".hd h1{color:#fff;margin:0;font-size:24px;font-weight:700;}"
            + ".hd p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:14px;}"
            + ".bd{padding:32px;}"
            + ".greet{color:#0D2118;font-size:15px;margin-bottom:16px;font-weight:600;}"
            + ".note{color:#3D6B52;font-size:13px;line-height:1.7;margin-bottom:22px;}"
            + ".box{background:#f0fdf4;border-radius:10px;padding:20px;margin:20px 0;"
            + "border:1px solid rgba(30,160,90,0.18);}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;"
            + "border-bottom:1px solid rgba(30,160,90,0.12);}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#5a8a6a;font-size:13px;}.val{color:#0D2118;font-size:13px;font-weight:700;}"
            + ".badge{background:#1A9E5A;color:#fff;border-radius:10px;padding:18px;"
            + "text-align:center;margin:22px 0;font-size:16px;font-weight:700;letter-spacing:.3px;}"
            + ".ft{background:#f0fdf4;border-top:1px solid rgba(30,160,90,0.12);padding:18px;"
            + "text-align:center;color:#8aaa9a;font-size:11px;}"
            + "</style></head><body>"
            + "<div class='w'>"
            + "<div class='hd'><h1>✅ You're all set!</h1><p>We'll see you at the park</p></div>"
            + "<div class='bd'>"
            + "<p class='greet'>Hi " + name + ",</p>"
            + "<p class='note'>Your visit has been confirmed — no further action needed. "
            + "Just show up and enjoy the park! Don't forget to bring your Order ID to the entrance.</p>"
            + "<div class='box'>"
            + "<div class='row'><span class='lbl'>Order #</span><span class='val'>" + orderId + "</span></div>"
            + "<div class='row'><span class='lbl'>Date</span><span class='val'>" + date + "</span></div>"
            + "<div class='row'><span class='lbl'>Time</span><span class='val'>" + time + "</span></div>"
            + "<div class='row'><span class='lbl'>Visitors</span><span class='val'>" + visitors + "</span></div>"
            + "</div>"
            + "<div class='badge'>Order #" + orderId + " — Show at entrance</div>"
            + "</div>"
            + "<div class='ft'>GoNature Parks Management System &nbsp;·&nbsp; Automated message &nbsp;·&nbsp; Do not reply</div>"
            + "</div></body></html>";
    }

    /** Response email sent when visitor clicks Cancel (from any email link). */
    public static String buildCancelEmail(String firstName, int orderId, String date, String time) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — Booking Cancelled</title>"
            + "<style>"
            + "body{margin:0;padding:40px 0;background:#edf7ee;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".w{max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
            + "border:1px solid rgba(30,160,90,0.20);overflow:hidden;"
            + "box-shadow:0 6px 24px rgba(0,60,30,0.10);}"
            + ".hd{background:linear-gradient(135deg,#C0303A,#E05060);padding:32px;text-align:center;}"
            + ".hd h1{color:#fff;margin:0;font-size:24px;font-weight:700;}"
            + ".hd p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:14px;}"
            + ".bd{padding:32px;}"
            + ".greet{color:#0D2118;font-size:15px;margin-bottom:16px;font-weight:600;}"
            + ".note{color:#3D6B52;font-size:13px;line-height:1.7;margin-bottom:22px;}"
            + ".box{background:#fff5f5;border-radius:10px;padding:20px;margin:20px 0;"
            + "border:1px solid rgba(192,48,58,0.18);}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;"
            + "border-bottom:1px solid rgba(192,48,58,0.10);}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#9a6a6a;font-size:13px;}.val{color:#3D0D0D;font-size:13px;font-weight:700;}"
            + ".ft{background:#f9f0f0;border-top:1px solid rgba(192,48,58,0.10);padding:18px;"
            + "text-align:center;color:#aaa;font-size:11px;}"
            + "</style></head><body>"
            + "<div class='w'>"
            + "<div class='hd'><h1>❌ Booking Cancelled</h1><p>We hope to see you another time</p></div>"
            + "<div class='bd'>"
            + "<p class='greet'>Hi " + name + ",</p>"
            + "<p class='note'>Your booking has been cancelled. If this was a mistake or you'd like to visit another time, "
            + "you're welcome to make a new booking anytime via the GoNature Guest Portal.</p>"
            + "<div class='box'>"
            + "<div class='row'><span class='lbl'>Cancelled Order #</span><span class='val'>" + orderId + "</span></div>"
            + "<div class='row'><span class='lbl'>Was scheduled for</span><span class='val'>" + date + " at " + time + "</span></div>"
            + "</div>"
            + "</div>"
            + "<div class='ft'>GoNature Parks Management System &nbsp;·&nbsp; Automated message &nbsp;·&nbsp; Do not reply</div>"
            + "</div></body></html>";
    }

    /** Builds the styled waitlist promotion HTML email body. */
    public static String buildWaitlistEmail(String firstName, int orderId, String date,
                                            String time, int visitors,
                                            String confirmUrl, String cancelUrl) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — Spot Available!</title>"
            + "<style>"
            + "body{margin:0;padding:40px 0;background:#edf7ee;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".w{max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
            + "border:1px solid rgba(30,160,90,0.20);overflow:hidden;"
            + "box-shadow:0 6px 24px rgba(0,60,30,0.10);}"
            + ".hd{background:linear-gradient(135deg,#1A9E5A,#2DC975);padding:32px;text-align:center;}"
            + ".hd h1{color:#fff;margin:0;font-size:24px;font-weight:700;letter-spacing:.5px;}"
            + ".hd p{color:rgba(255,255,255,.85);margin:6px 0 0;font-size:14px;}"
            + ".bd{padding:32px;}"
            + ".greet{color:#0D2118;font-size:15px;margin-bottom:16px;}"
            + ".note{color:#3D6B52;font-size:13px;line-height:1.7;margin-bottom:22px;}"
            + ".box{background:#f0fdf4;border-radius:10px;padding:20px;margin:20px 0;"
            + "border:1px solid rgba(30,160,90,0.18);}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;"
            + "border-bottom:1px solid rgba(30,160,90,0.12);}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#5a8a6a;font-size:13px;}.val{color:#0D2118;font-size:13px;font-weight:700;}"
            + ".warn{background:#fffbeb;border:1px solid #f59e0b;border-radius:8px;padding:14px;"
            + "color:#92400e;font-size:13px;text-align:center;margin:20px 0;}"
            + "a.btn-c{display:block;background:#1A9E5A;color:#fff!important;text-decoration:none;"
            + "text-align:center;padding:16px 30px;border-radius:10px;font-size:16px;font-weight:700;"
            + "margin:22px 0 12px;letter-spacing:.3px;}"
            + "a.btn-x{display:block;text-align:center;color:#5a8a6a!important;text-decoration:none;"
            + "font-size:13px;padding:8px;}"
            + ".ft{background:#f0fdf4;border-top:1px solid rgba(30,160,90,0.12);padding:18px;"
            + "text-align:center;color:#8aaa9a;font-size:11px;}"
            + "</style></head><body>"
            + "<div class='w'>"
            + "<div class='hd'><h1>🎉 A spot opened up!</h1><p>You're off the waitlist — act now!</p></div>"
            + "<div class='bd'>"
            + "<p class='greet'>Hello <strong style='color:#0D2118'>" + firstName + "</strong>,</p>"
            + "<p class='note'>Great news! A spot has opened for your visit. You now have "
            + "<strong style='color:#d97706'>1 hour</strong> to confirm, or the spot will be offered to the next person on the waitlist.</p>"
            + "<div class='box'>"
            + "<div class='row'><span class='lbl'>Order #</span><span class='val'>" + orderId + "</span></div>"
            + "<div class='row'><span class='lbl'>Date</span><span class='val'>" + date + "</span></div>"
            + "<div class='row'><span class='lbl'>Time</span><span class='val'>" + time + "</span></div>"
            + "<div class='row'><span class='lbl'>Visitors</span><span class='val'>" + visitors + "</span></div>"
            + "</div>"
            + "<div class='warn'>⏳ Confirm within <strong>1 hour</strong> or the spot is given to the next person.</div>"
            + "<a href='" + confirmUrl + "' class='btn-c'>✅ Confirm My Spot Now</a>"
            + "<a href='" + cancelUrl  + "' class='btn-x'>I can't make it — Cancel</a>"
            + "</div>"
            + "<div class='ft'>GoNature Parks Management System &nbsp;·&nbsp; Automated message &nbsp;·&nbsp; Do not reply</div>"
            + "</div></body></html>";
    }
}
