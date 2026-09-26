package controller;

import com.google.gson.JsonObject;
import model.Cart;
import model.CartItem;
import model.Restaurant;
import service.CartService;
import service.RestaurantService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CartServlet — manages the customer's shopping cart.
 *
 * Endpoints:
 *   GET    /api/cart             → view current cart items + total
 *   POST   /api/cart/add         → add item to cart
 *   PUT    /api/cart/update      → update item quantity
 *   DELETE /api/cart/remove      → remove item from cart
 *
 * All endpoints require an authenticated session (userId must be present).
 * Responsibility: Read input → call CartService → return JSON.
 */
@WebServlet("/api/cart/*")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();
    private final RestaurantService restaurantService = new RestaurantService();

    // ─────────────── GET /api/cart ───────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) return;

        List<CartItem> items = cartService.viewCart(userId);
        BigDecimal total = cartService.getCartTotal(userId);

        // Enrich with restaurant name for the cart header display
        Cart activeCart = cartService.getActiveCart(userId);
        String restaurantName = null;
        if (activeCart != null) {
            Restaurant restaurant = restaurantService.getRestaurantById(activeCart.getRestaurantId());
            if (restaurant != null) {
                restaurantName = restaurant.getName();
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items",          items);
        data.put("total",          total);
        data.put("itemCount",      items.size());
        data.put("restaurantName", restaurantName);

        JsonUtil.sendSuccess(response, "Cart fetched.", data);
    }

    // ─────────────── POST /api/cart/add ───────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) return;

        String path = request.getPathInfo(); // "/add"

        if ("/add".equals(path)) {
            JsonObject body = parseJsonBody(request);
            if (body == null) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body.");
                return;
            }

            if (!body.has("menuItemId") || !body.has("quantity")) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "menuItemId and quantity are required.");
                return;
            }

            int menuItemId = body.get("menuItemId").getAsInt();
            int quantity   = body.get("quantity").getAsInt();

            if (quantity < 1) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Quantity must be at least 1.");
                return;
            }

            boolean success = cartService.addItemToCart(userId, menuItemId, quantity);

            if (success) {
                JsonUtil.sendSuccess(response, "Item added to cart.", null);
            } else {
                // CartService prints the reason; common causes:
                // - item unavailable
                // - item from different restaurant than existing cart
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Could not add item. The item may be unavailable, or your cart already " +
                        "contains items from a different restaurant. Please clear your cart first.");
            }

        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─────────────── PUT /api/cart/update ───────────────
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) return;

        String path = request.getPathInfo(); // "/update"

        if ("/update".equals(path)) {
            JsonObject body = parseJsonBody(request);
            if (body == null) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body.");
                return;
            }

            if (!body.has("cartItemId") || !body.has("quantity")) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "cartItemId and quantity are required.");
                return;
            }

            int cartItemId  = body.get("cartItemId").getAsInt();
            int newQuantity = body.get("quantity").getAsInt();

            if (newQuantity < 1) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Quantity must be at least 1. Use remove to delete an item.");
                return;
            }

            boolean success = cartService.updateItemQuantity(userId, cartItemId, newQuantity);

            if (success) {
                // Return updated total so frontend can refresh without an extra call
                BigDecimal total = cartService.getCartTotal(userId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("newTotal", total);
                JsonUtil.sendSuccess(response, "Quantity updated.", data);
            } else {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Could not update quantity. Item may not belong to your cart.");
            }

        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─────────────── DELETE /api/cart/remove ───────────────
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) return;

        String path = request.getPathInfo(); // "/remove"

        if ("/remove".equals(path)) {
            String cartItemIdParam = request.getParameter("cartItemId");

            if (cartItemIdParam == null || cartItemIdParam.isBlank()) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "cartItemId query parameter is required.");
                return;
            }

            int cartItemId;
            try {
                cartItemId = Integer.parseInt(cartItemIdParam.trim());
            } catch (NumberFormatException e) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid cartItemId.");
                return;
            }

            boolean success = cartService.removeItem(userId, cartItemId);

            if (success) {
                BigDecimal total = cartService.getCartTotal(userId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("newTotal", total);
                JsonUtil.sendSuccess(response, "Item removed from cart.", data);
            } else {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Could not remove item. It may not belong to your cart.");
            }

        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─────────────── HELPERS ───────────────

    /**
     * Check for a valid authenticated session.
     * Returns the userId if authenticated, or null after writing a 401 response.
     */
    private Integer getAuthenticatedUserId(HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "You must be logged in to access the cart.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    /** Read and parse the request body as a Gson JsonObject. */
    private JsonObject parseJsonBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return JsonUtil.getGson().fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }
}
