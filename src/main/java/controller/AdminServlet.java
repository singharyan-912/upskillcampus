package controller;

import config.DatabaseConnection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminServlet — read-only dashboard APIs exclusively for ADMIN role.
 *
 * Endpoints:
 *   GET /api/admin/stats        → total users, restaurants, orders, revenue, deliveries
 *   GET /api/admin/users        → all users (password excluded)
 *   GET /api/admin/restaurants  → all restaurants
 *   GET /api/admin/orders       → all orders enriched with restaurant name
 *   GET /api/admin/deliveries   → all deliveries enriched with order + partner info
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    // ─── GET ────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireAdmin(req, resp);
        if (userId == null) return;

        String path = req.getPathInfo();

        switch (path != null ? path : "") {
            case "/stats":
                JsonUtil.sendSuccess(resp, "Stats fetched.", getStats());
                break;
            case "/users":
                JsonUtil.sendSuccess(resp, "Users fetched.", getAllUsers());
                break;
            case "/restaurants":
                JsonUtil.sendSuccess(resp, "Restaurants fetched.", getAllRestaurants());
                break;
            case "/orders":
                JsonUtil.sendSuccess(resp, "Orders fetched.", getAllOrders());
                break;
            case "/deliveries":
                JsonUtil.sendSuccess(resp, "Deliveries fetched.", getAllDeliveries());
                break;
            default:
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private Integer requireAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        String role = String.valueOf(session.getAttribute("userRole"));
        if (!"ADMIN".equals(role)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "ADMIN role required.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    private Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        String[] sqls = {
            "SELECT COUNT(*) FROM users",
            "SELECT COUNT(*) FROM restaurants",
            "SELECT COUNT(*) FROM orders",
            "SELECT COUNT(*) FROM deliveries",
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS'"
        };
        String[] keys = {"totalUsers","totalRestaurants","totalOrders","totalDeliveries","totalRevenue"};
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int i = 0; i < sqls.length; i++) {
                try (PreparedStatement stmt = conn.prepareStatement(sqls[i]);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        if (i == 4) stats.put(keys[i], rs.getBigDecimal(1));
                        else        stats.put(keys[i], rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    private List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT user_id, name, email, phone, role FROM users ORDER BY user_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", rs.getInt("user_id"));
                row.put("name",   rs.getString("name"));
                row.put("email",  rs.getString("email"));
                row.put("phone",  rs.getString("phone"));
                row.put("role",   rs.getString("role"));
                result.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    private List<Map<String, Object>> getAllRestaurants() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT r.restaurant_id, r.name, r.address, r.phone, r.cuisine_type, " +
                     "r.status, r.created_at, u.name AS owner_name " +
                     "FROM restaurants r JOIN users u ON r.owner_id = u.user_id " +
                     "ORDER BY r.restaurant_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("restaurantId", rs.getInt("restaurant_id"));
                row.put("name",         rs.getString("name"));
                row.put("address",      rs.getString("address"));
                row.put("phone",        rs.getString("phone"));
                row.put("cuisineType",  rs.getString("cuisine_type"));
                row.put("status",       rs.getString("status"));
                row.put("createdAt",    rs.getTimestamp("created_at"));
                row.put("ownerName",    rs.getString("owner_name"));
                result.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    private List<Map<String, Object>> getAllOrders() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, o.total_amount, o.status, " +
                     "o.delivery_address, o.created_at, " +
                     "r.name AS restaurant_name, u.name AS customer_name " +
                     "FROM orders o " +
                     "JOIN restaurants r ON o.restaurant_id = r.restaurant_id " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "ORDER BY o.order_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId",          rs.getInt("order_id"));
                row.put("customerId",       rs.getInt("user_id"));
                row.put("customerName",     rs.getString("customer_name"));
                row.put("restaurantName",   rs.getString("restaurant_name"));
                row.put("totalAmount",      rs.getBigDecimal("total_amount"));
                row.put("status",           rs.getString("status"));
                row.put("deliveryAddress",  rs.getString("delivery_address"));
                row.put("createdAt",        rs.getTimestamp("created_at"));
                result.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    private List<Map<String, Object>> getAllDeliveries() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT d.delivery_id, d.order_id, d.delivery_status, " +
                     "d.assigned_time, d.delivered_time, " +
                     "u.name AS partner_name, " +
                     "r.name AS restaurant_name, " +
                     "o.delivery_address, o.total_amount " +
                     "FROM deliveries d " +
                     "JOIN users u ON d.delivery_partner_id = u.user_id " +
                     "JOIN orders o ON d.order_id = o.order_id " +
                     "JOIN restaurants r ON o.restaurant_id = r.restaurant_id " +
                     "ORDER BY d.delivery_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("deliveryId",      rs.getInt("delivery_id"));
                row.put("orderId",         rs.getInt("order_id"));
                row.put("deliveryStatus",  rs.getString("delivery_status"));
                row.put("partnerName",     rs.getString("partner_name"));
                row.put("restaurantName",  rs.getString("restaurant_name"));
                row.put("deliveryAddress", rs.getString("delivery_address"));
                row.put("totalAmount",     rs.getBigDecimal("total_amount"));
                row.put("assignedTime",    rs.getTimestamp("assigned_time"));
                row.put("deliveredTime",   rs.getTimestamp("delivered_time"));
                result.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
}
