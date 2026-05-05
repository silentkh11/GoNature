package server;
import firstPackage.Order;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBController {
    
    private static Connection conn;

    // Call this method when your Server GUI starts
    public static void connectToDB() {
        try {
            // Register the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connect to the DB (Change the password to your actual MySQL root password!)
            // Also ensure the timezone is set to avoid errors
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/gonature?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "Hamkh1324@@%%");
            
            System.out.println("SQL connection succeed");
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } catch (ClassNotFoundException e) {
            System.out.println("Could not find the JDBC driver.");
            e.printStackTrace();
        }
    }
    
    // We will add getOrders() and updateOrder() here later!
    
    public static ArrayList<Order> getOrders() {
        ArrayList<Order> list = new ArrayList<>();
        try {
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM `Order`");
            while(rs.next()) {
                Order order = new Order(
                    rs.getInt("order_number"), rs.getDate("order_date"),
                    rs.getInt("number_of_visitors"), rs.getInt("confirmation_code"),
                    rs.getInt("subscriber_id"), rs.getDate("date_of_placing_order")
                );
                list.add(order);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static void updateOrder(Order order) {
        try {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "UPDATE `Order` SET order_date = ?, number_of_visitors = ? WHERE order_number = ?"
            );
            ps.setDate(1, order.getOrderDate());
            ps.setInt(2, order.getNumberOfVisitors());
            ps.setInt(3, order.getOrderNumber());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}