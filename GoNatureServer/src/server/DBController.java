package server;

import firstPackage.Order;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBController {
    
    // The Pool Manager
    private static HikariDataSource dataSource;

    public static void connectToDB() throws SQLException {
        try {
            HikariConfig config = new HikariConfig();
            
            // Your exact database credentials
            config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword("12345678"); // Change this if your DB password is different!
            
            // Pool Configuration
            config.setMaximumPoolSize(10); // Hold 10 connections ready
            config.setMinimumIdle(2);      // Always keep at least 2 open and waiting
            config.setConnectionTimeout(30000); // 30 second timeout
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Build the pool
            dataSource = new HikariDataSource(config);
            System.out.println("SQL Connection Pool initialized successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Connection Pool!");
            e.printStackTrace();
            throw new SQLException("Pool initialization failed", e);
        }
    }

    public static ArrayList<Order> getOrders() {
        ArrayList<Order> list = new ArrayList<>();
        String query = "SELECT * FROM `Order`";
        
        // try-with-resources automatically borrows and returns the connection
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
             
            while(rs.next()) {
                Order order = new Order(
                    rs.getInt("order_number"), rs.getDate("order_date"),
                    rs.getInt("number_of_visitors"), rs.getInt("confirmation_code"),
                    rs.getInt("subscriber_id"), rs.getDate("date_of_placing_order")
                );
                list.add(order);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return list;
    }

    public static void updateOrder(Order order) {
        String query = "UPDATE `Order` SET order_date = ?, number_of_visitors = ? WHERE order_number = ?";
        
        // try-with-resources automatically borrows and returns the connection
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
             
            ps.setDate(1, order.getOrderDate());
            ps.setInt(2, order.getNumberOfVisitors());
            ps.setInt(3, order.getOrderNumber());
            ps.executeUpdate();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
}