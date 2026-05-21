package server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import entities.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBController {
    
    // The Pool Manager
    private static HikariDataSource dataSource;

    public static void connectToDB() throws Exception {
        try {
            HikariConfig config = new HikariConfig();
            
            // Your exact database credentials
            config.setJdbcUrl("jdbc:mysql://localhost:3306/gonature?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword("root"); // Updated to your password!
            
            // Pool Configuration
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Build the pool
            dataSource = new HikariDataSource(config);
            System.out.println("SQL Connection Pool initialized successfully!");
            
        } catch (Exception e) {
            // We removed e.printStackTrace() so it doesn't spam the console!
            // Instead, we throw a clean, single-line error message back to the UI.
            throw new Exception("Database Authentication Failed: Check your SQL username and password.");
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