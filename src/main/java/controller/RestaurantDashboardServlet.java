package controller;

import com.google.gson.JsonObject;
import model.MenuItem;
import model.Restaurant;
import service.MenuItemService;
import service.RestaurantService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * RestaurantDashboardServlet — APIs exclusively for RESTAURANT role.
 *
 * Endpoints:
 *   GET    /api/restaurant/mine                  → restaurants owned by session user
 *   GET    /api/restaurant/menu?restaurantId=    → ALL menu items (incl unavailable)
 *   POST   /api/restaurant/menu                  → add menu item
 *   PUT    /api/restaurant/menu/{id}             → update menu item
 *   PUT    /api/restaurant/menu/{id}/availability → toggle availability
 *   DELETE /api/restaurant/menu/{id}             → delete menu item
 *   PUT    /api/restaurant/info/{id}             → update restaurant info
 */
@WebServlet("/api/restaurant/*")
public class RestaurantDashboardServlet extends HttpServlet {

    private final RestaurantService restaurantService = new RestaurantService();
    private final MenuItemService   menuItemService   = new MenuItemService();

    // ─── GET ────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireRestaurant(req, resp);
        if (userId == null) return;

        String path = req.getPathInfo();

        if ("/mine".equals(path)) {
            List<Restaurant> restaurants = restaurantService.getRestaurantsByOwnerId(userId);
            JsonUtil.sendSuccess(resp, "Restaurants fetched.", restaurants);

        } else if ("/menu".equals(path)) {
            String restIdParam = req.getParameter("restaurantId");
            if (restIdParam == null || restIdParam.isBlank()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "restaurantId parameter is required.");
                return;
            }
            int restaurantId;
            try { restaurantId = Integer.parseInt(restIdParam.trim()); }
            catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid restaurantId.");
                return;
            }
            Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
            if (restaurant == null || restaurant.getOwnerId() != userId) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Access denied. You do not own this restaurant.");
                return;
            }
            List<MenuItem> items = menuItemService.getMenuByRestaurantId(restaurantId);
            JsonUtil.sendSuccess(resp, "Menu items fetched.", items);

        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── POST ───────────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireRestaurant(req, resp);
        if (userId == null) return;

        if ("/menu".equals(req.getPathInfo())) {
            JsonObject body = parseJsonBody(req);
            if (body == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body.");
                return;
            }

            int     restaurantId = getIntSafe(body, "restaurantId");
            String  itemName     = getStringSafe(body, "itemName");
            String  description  = getStringSafe(body, "description");
            String  category     = getStringSafe(body, "category");
            double  priceVal     = getDoubleSafe(body, "price");
            boolean available    = getBooleanSafe(body, "available", true);

            if (restaurantId <= 0 || itemName.isEmpty() || category.isEmpty() || priceVal <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "restaurantId, itemName, category, and price (> 0) are required.");
                return;
            }

            MenuItem item = new MenuItem(restaurantId, itemName, description,
                    BigDecimal.valueOf(priceVal), category, available);

            boolean success = menuItemService.addMenuItem(item, userId);
            if (success) {
                JsonUtil.sendSuccess(resp, "Menu item added successfully.", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Failed to add item. Check ownership and field values.");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── PUT ────────────────────────────────────────────────────────────────
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireRestaurant(req, resp);
        if (userId == null) return;

        String path = req.getPathInfo();
        if (path == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
            return;
        }

        // PUT /api/restaurant/menu/{id}/availability
        if (path.endsWith("/availability")) {
            String[] parts = path.split("/");
            int menuItemId = parseIdFromParts(parts, 2, resp);
            if (menuItemId < 0) return;

            JsonObject body = parseJsonBody(req);
            if (body == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body."); return;
            }
            boolean available = getBooleanSafe(body, "available", true);
            boolean success   = menuItemService.updateAvailability(menuItemId, available, userId);
            if (success) JsonUtil.sendSuccess(resp, "Availability updated.", null);
            else         JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Failed to update availability.");
            return;
        }

        // PUT /api/restaurant/menu/{id}
        if (path.startsWith("/menu/")) {
            String[] parts   = path.split("/");
            int menuItemId   = parseIdFromParts(parts, 2, resp);
            if (menuItemId < 0) return;

            JsonObject body = parseJsonBody(req);
            if (body == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body."); return;
            }

            String  itemName    = getStringSafe(body, "itemName");
            String  description = getStringSafe(body, "description");
            String  category    = getStringSafe(body, "category");
            double  priceVal    = getDoubleSafe(body, "price");
            boolean available   = getBooleanSafe(body, "available", true);

            if (itemName.isEmpty() || category.isEmpty() || priceVal <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "itemName, category, and price (> 0) are required.");
                return;
            }

            MenuItem item = new MenuItem();
            item.setMenuItemId(menuItemId);
            item.setItemName(itemName);
            item.setDescription(description);
            item.setCategory(category);
            item.setPrice(BigDecimal.valueOf(priceVal));
            item.setAvailability(available);

            boolean success = menuItemService.updateMenuItem(item, userId);
            if (success) JsonUtil.sendSuccess(resp, "Menu item updated.", null);
            else         JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Failed to update item.");
            return;
        }

        // PUT /api/restaurant/info/{id}
        if (path.startsWith("/info/")) {
            String[] parts = path.split("/");
            int restaurantId = parseIdFromParts(parts, 2, resp);
            if (restaurantId < 0) return;

            Restaurant existing = restaurantService.getRestaurantById(restaurantId);
            if (existing == null || existing.getOwnerId() != userId) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied."); return;
            }

            JsonObject body = parseJsonBody(req);
            if (body == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body."); return;
            }

            String name        = getStringSafe(body, "name");
            String address     = getStringSafe(body, "address");
            String phone       = getStringSafe(body, "phone");
            String cuisineType = getStringSafe(body, "cuisineType");
            String status      = getStringSafe(body, "status");

            if (name.isEmpty() || address.isEmpty() || phone.isEmpty() || cuisineType.isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "name, address, phone, and cuisineType are required.");
                return;
            }
            if (status.isEmpty()) status = existing.getStatus();

            Restaurant updated = new Restaurant(userId, name, address, phone, cuisineType, status);
            updated.setRestaurantId(restaurantId);

            boolean success = restaurantService.updateRestaurant(updated);
            if (success) JsonUtil.sendSuccess(resp, "Restaurant updated.", null);
            else         JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Failed to update restaurant.");
            return;
        }

        JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Integer userId = requireRestaurant(req, resp);
        if (userId == null) return;

        String path = req.getPathInfo();
        if (path != null && path.startsWith("/menu/")) {
            String[] parts = path.split("/");
            int menuItemId = parseIdFromParts(parts, 2, resp);
            if (menuItemId < 0) return;

            boolean success = menuItemService.deleteMenuItem(menuItemId, userId);
            if (success) JsonUtil.sendSuccess(resp, "Menu item deleted.", null);
            else         JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Failed to delete. Check ownership.");
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private Integer requireRestaurant(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        String role = String.valueOf(session.getAttribute("userRole"));
        if (!"RESTAURANT".equals(role)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "RESTAURANT role required.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    private int parseIdFromParts(String[] parts, int index, HttpServletResponse resp)
            throws IOException {
        if (parts.length <= index) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing ID in path.");
            return -1;
        }
        try { return Integer.parseInt(parts[index]); }
        catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID.");
            return -1;
        }
    }

    private JsonObject parseJsonBody(HttpServletRequest req) {
        try (BufferedReader reader = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return JsonUtil.getGson().fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) { return null; }
    }

    private String  getStringSafe(JsonObject o, String k)   { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString().trim() : ""; }
    private int     getIntSafe(JsonObject o, String k)      { try { return o.has(k) ? o.get(k).getAsInt()    : 0; } catch (Exception e) { return 0; } }
    private double  getDoubleSafe(JsonObject o, String k)   { try { return o.has(k) ? o.get(k).getAsDouble() : 0; } catch (Exception e) { return 0; } }
    private boolean getBooleanSafe(JsonObject o, String k, boolean def) { try { return o.has(k) ? o.get(k).getAsBoolean() : def; } catch (Exception e) { return def; } }
}
