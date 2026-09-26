package controller;

import com.google.gson.JsonObject;
import config.DatabaseConnection;
import model.Delivery;
import model.Order;
import service.DeliveryService;
import service.OrderService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeliveryServlet — APIs exclusively for DELIVERY_PARTNER role.
 *
 * Endpoints:
 *   GET /api/delivery/mine          → deliveries assigned to this partner
 *   GET /api/delivery/available     → READY orders not yet assigned
 *   PUT /api/delivery/status        → update delivery status
 *   POST /api/delivery/assign       → self-assign a READY order
 */
@WebServlet("/api/delivery/*")
public class DeliveryServlet extends HttpServlet {

    private final DeliveryService deliveryService = new DeliveryService();
    private final OrderService    orderService    = new OrderService();

    // ─── GET ────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireDeliveryPartner(req, resp);
        if (userId == null) return;

        String path = req.getPathInfo();

        if ("/mine".equals(path)) {
            // All deliveries assigned to this partner, enriched with order + address info
            List<Map<String, Object>> deliveries = getDeliveriesForPartner(userId);
            JsonUtil.sendSuccess(resp, "Deliveries fetched.", deliveries);

        } else if ("/available".equals(path)) {
            // READY orders not yet assigned — partner can self-assign
            List<Map<String, Object>> available = getAvailableOrders();
            JsonUtil.sendSuccess(resp, "Available orders fetched.", available);

        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── POST ───────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireDeliveryPartner(req, resp);
        if (userId == null) return;

        if ("/assign".equals(req.getPathInfo())) {
            JsonObject body = parseJsonBody(req);
            if (body == null || !body.has("orderId")) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "orderId is required.");
                return;
            }
            int orderId = body.get("orderId").getAsInt();
            boolean success = deliveryService.assignDelivery(userId, orderId);
            if (success) {
                JsonUtil.sendSuccess(resp, "Delivery assigned successfully.", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Failed to assign. Order may not be READY or already assigned.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── PUT ────────────────────────────────────────────────────────────────
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireDeliveryPartner(req, resp);
        if (userId == null) return;

        if ("/status".equals(req.getPathInfo())) {
            JsonObject body = parseJsonBody(req);
            if (body == null || !body.has("orderId") || !body.has("newStatus")) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "orderId and newStatus are required.");
                return;
            }
            int    orderId   = body.get("orderId").getAsInt();
            String newStatus = body.get("newStatus").getAsString().trim();

            boolean success = deliveryService.updateDeliveryStatus(userId, orderId, newStatus);
            if (success) {
                JsonUtil.sendSuccess(resp, "Delivery status updated to " + newStatus + ".", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid status transition or access denied.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private Integer requireDeliveryPartner(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        String role = String.valueOf(session.getAttribute("userRole"));
        if (!"DELIVERY_PARTNER".equals(role)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "DELIVERY_PARTNER role required.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    /** Returns enriched list of deliveries for a given partner. */
    private List<Map<String, Object>> getDeliveriesForPartner(int partnerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT d.delivery_id, d.order_id, d.delivery_status, " +
                     "d.assigned_time, d.pickup_time, d.delivered_time, " +
                     "o.delivery_address, o.total_amount, o.status AS order_status, " +
                     "r.name AS restaurant_name, r.address AS restaurant_address " +
                     "FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.order_id " +
                     "JOIN restaurants r ON o.restaurant_id = r.restaurant_id " +
                     "WHERE d.delivery_partner_id = ? " +
                     "ORDER BY d.assigned_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partnerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("deliveryId",         rs.getInt("delivery_id"));
                    row.put("orderId",            rs.getInt("order_id"));
                    row.put("deliveryStatus",     rs.getString("delivery_status"));
                    row.put("assignedTime",       rs.getTimestamp("assigned_time"));
                    row.put("pickupTime",         rs.getTimestamp("pickup_time"));
                    row.put("deliveredTime",      rs.getTimestamp("delivered_time"));
                    row.put("deliveryAddress",    rs.getString("delivery_address"));
                    row.put("totalAmount",        rs.getBigDecimal("total_amount"));
                    row.put("orderStatus",        rs.getString("order_status"));
                    row.put("restaurantName",     rs.getString("restaurant_name"));
                    row.put("restaurantAddress",  rs.getString("restaurant_address"));
                    result.add(row);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    /** Returns READY orders not yet in the deliveries table. */
    private List<Map<String, Object>> getAvailableOrders() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT o.order_id, o.total_amount, o.delivery_address, o.created_at, " +
                     "r.name AS restaurant_name, r.address AS restaurant_address " +
                     "FROM orders o " +
                     "JOIN restaurants r ON o.restaurant_id = r.restaurant_id " +
                     "WHERE o.status = 'READY' " +
                     "AND o.order_id NOT IN (SELECT order_id FROM deliveries) " +
                     "ORDER BY o.created_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("orderId",           rs.getInt("order_id"));
                row.put("totalAmount",       rs.getBigDecimal("total_amount"));
                row.put("deliveryAddress",   rs.getString("delivery_address"));
                row.put("createdAt",         rs.getTimestamp("created_at"));
                row.put("restaurantName",    rs.getString("restaurant_name"));
                row.put("restaurantAddress", rs.getString("restaurant_address"));
                result.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    private JsonObject parseJsonBody(HttpServletRequest req) {
        try (BufferedReader reader = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return JsonUtil.getGson().fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) { return null; }
    }
}
