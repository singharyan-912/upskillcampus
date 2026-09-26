package service;

import config.DatabaseConnection;
import dao.UserDAO;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminService {

    private final UserDAO userDAO = new UserDAO();

    public void printSystemStatistics(int adminUserId) {
        // Verify admin role
        User admin = userDAO.getUserById(adminUserId);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            System.out.println("Access denied. Only ADMIN users can view statistics.");
            return;
        }

        System.out.println("--- SYSTEM STATISTICS DASHBOARD ---");

        try (Connection connection = DatabaseConnection.getConnection()) {

            // 1. Total Users
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total Users: " + rs.getInt(1));
                }
            }

            // 2. Total Restaurants
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM restaurants");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total Restaurants: " + rs.getInt(1));
                }
            }

            // 3. Total Orders
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM orders");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total Orders: " + rs.getInt(1));
                }
            }
            
            // 4. Total Deliveries
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM deliveries");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Total Deliveries: " + rs.getInt(1));
                }
            }

            // 5. Total Revenue (SUCCESS payments)
            try (PreparedStatement stmt = connection.prepareStatement("SELECT SUM(amount) FROM payments WHERE payment_status = 'SUCCESS'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal revenue = rs.getBigDecimal(1);
                    if (revenue == null) {
                        revenue = java.math.BigDecimal.ZERO;
                    }
                    System.out.println("Total Revenue (Successful Payments): Rs." + revenue);
                }
            }

        } catch (SQLException e) {
            System.out.println("Failed to fetch statistics.");
            e.printStackTrace();
        }
        System.out.println("-----------------------------------");
    }
}
