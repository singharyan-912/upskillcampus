package controller;

import com.google.gson.Gson;
import model.Order;
import model.OrderItem;
import model.Restaurant;
import service.OrderService;
import service.RestaurantService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OrderServlet — manages customer orders.
 *
 * Endpoints:
 *   POST /api/orders/checkout          → create order from active cart
 *   GET  /api/orders                   → list orders for the logged-in customer
 *   GET  /api/orders/{orderId}         → get order details + items
 *   PUT  /api/orders/status            → update order status (RESTAURANT role)
 *
 * Session attributes used (set by AuthServlet):
 *   "userId"   (Integer)
 *   "userRole" (String)
 */
@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {

    private final OrderService     orderService     = new OrderService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final Gson              gson             = new Gson();

    // ─────────────── GET ───────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;
        String role = getAuthRole(req);

        String pathInfo = req.getPathInfo(); // null | "/" | "/{orderId}"

        try {
            // GET /api/orders  OR  /api/orders/
            if (pathInfo == null || pathInfo.equals("/")) {
                if ("CUSTOMER".equals(role)) {
                    List<Order> orders = orderService.getOrdersByUserId(userId);
                    // Enrich each order with restaurant name
                    List<Map<String, Object>> enriched = enrichOrders(orders);
                    JsonUtil.sendSuccess(resp, "Orders retrieved.", enriched);

                } else if ("RESTAURANT".equals(role)) {
                    String restIdParam = req.getParameter("restaurantId");
                    if (restIdParam == null) {
                        JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                "restaurantId parameter required.");
                        return;
                    }
                    int restaurantId = Integer.parseInt(restIdParam);
                    List<Order> orders = orderService.getOrdersByRestaurantId(userId, restaurantId);
                    if (orders != null) {
                        JsonUtil.sendSuccess(resp, "Restaurant orders retrieved.", enrichOrders(orders));
                    } else {
                        JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                                "Access denied or restaurant not found.");
                    }
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                }
                return;
            }

            // GET /api/orders/{orderId}
            String[] parts = pathInfo.split("/"); // ["", "42"]
            if (parts.length == 2) {
                int orderId = Integer.parseInt(parts[1]);
                Order order = orderService.getOrderById(orderId);

                // Customers can only view their own orders; restaurants can view any of their orders
                if (order == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Order not found.");
                    return;
                }
                if ("CUSTOMER".equals(role) && order.getUserId() != userId) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                    return;
                }

                List<OrderItem> items = orderService.getOrderItemsByOrderId(orderId);

                // Enrich with restaurant name
                String restaurantName = null;
                Restaurant restaurant = restaurantService.getRestaurantById(order.getRestaurantId());
                if (restaurant != null) restaurantName = restaurant.getName();

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("order",          order);
                data.put("items",          items);
                data.put("restaurantName", restaurantName);

                JsonUtil.sendSuccess(resp, "Order details retrieved.", data);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
            }

        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format.");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.");
        }
    }

    // ─────────────── POST ───────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;
        String role = getAuthRole(req);

        String pathInfo = req.getPathInfo();

        if ("/checkout".equals(pathInfo)) {
            if (!"CUSTOMER".equals(role)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Only customers can checkout.");
                return;
            }

            try {
                CheckoutRequest checkoutReq = gson.fromJson(req.getReader(), CheckoutRequest.class);
                if (checkoutReq == null
                        || checkoutReq.deliveryAddress == null
                        || checkoutReq.deliveryAddress.trim().isEmpty()) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Delivery address is required.");
                    return;
                }

                Order newOrder = orderService.checkout(userId, checkoutReq.deliveryAddress.trim());
                if (newOrder != null) {
                    // Also attach restaurant name to the success payload
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("orderId",      newOrder.getOrderId());
                    data.put("totalAmount",  newOrder.getTotalAmount());
                    data.put("status",       newOrder.getStatus());
                    data.put("deliveryAddress", newOrder.getDeliveryAddress());

                    Restaurant restaurant = restaurantService.getRestaurantById(newOrder.getRestaurantId());
                    data.put("restaurantName",
                            restaurant != null ? restaurant.getName() : "Restaurant");

                    JsonUtil.sendSuccess(resp, "Order placed successfully!", data);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Failed to place order. Your cart may be empty.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred.");
            }

        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
        }
    }

    // ─────────────── PUT ───────────────
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;
        String role = getAuthRole(req);

        String pathInfo = req.getPathInfo();

        if ("/status".equals(pathInfo)) {
            if (!"RESTAURANT".equals(role)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Only restaurant owners can update order status.");
                return;
            }

            try {
                UpdateStatusRequest updateReq =
                        gson.fromJson(req.getReader(), UpdateStatusRequest.class);
                if (updateReq == null || updateReq.newStatus == null
                        || updateReq.orderId <= 0 || updateReq.restaurantId <= 0) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "orderId, restaurantId, and newStatus are required.");
                    return;
                }

                boolean success = orderService.updateOrderStatusByRestaurant(
                        userId, updateReq.orderId, updateReq.restaurantId, updateReq.newStatus);

                if (success) {
                    JsonUtil.sendSuccess(resp,
                            "Order status updated to " + updateReq.newStatus + ".", null);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Failed to update status. Invalid transition or access denied.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred.");
            }

        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
        }
    }

    // ─────────────── HELPERS ───────────────

    /** Returns userId from session, or null + 401 response if not authenticated. */
    private Integer getAuthUserId(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    /** Returns role string from session (safe — never null after getAuthUserId passes). */
    private String getAuthRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return "";
        Object role = session.getAttribute("userRole");
        return role != null ? role.toString() : "";
    }

    /**
     * Enrich a list of orders with restaurant name so the frontend
     * doesn't need a second round-trip per order.
     */
    private List<Map<String, Object>> enrichOrders(List<Order> orders) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (orders == null) return result;

        for (Order o : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId",        o.getOrderId());
            map.put("userId",         o.getUserId());
            map.put("restaurantId",   o.getRestaurantId());
            map.put("totalAmount",    o.getTotalAmount());
            map.put("status",         o.getStatus());
            map.put("deliveryAddress", o.getDeliveryAddress());
            map.put("createdAt",      o.getCreatedAt());

            Restaurant restaurant = restaurantService.getRestaurantById(o.getRestaurantId());
            map.put("restaurantName",
                    restaurant != null ? restaurant.getName() : "Restaurant");

            result.add(map);
        }
        return result;
    }

    // ─────────────── JSON body DTOs ───────────────

    private static class CheckoutRequest {
        String deliveryAddress;
    }

    private static class UpdateStatusRequest {
        int    orderId;
        int    restaurantId;
        String newStatus;
    }
}
