package controller;

import model.MenuItem;
import service.MenuItemService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MenuServlet — exposes menu item data for a given restaurant.
 *
 * Endpoints:
 *   GET /api/menu?restaurantId={id}   → list available menu items for a restaurant
 *
 * Responsibility: Read request → call MenuItemService → return JSON.
 */
@WebServlet("/api/menu")
public class MenuServlet extends HttpServlet {

    private final MenuItemService menuItemService = new MenuItemService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String restaurantIdParam = request.getParameter("restaurantId");

        if (restaurantIdParam == null || restaurantIdParam.isBlank()) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "restaurantId parameter is required.");
            return;
        }

        int restaurantId;
        try {
            restaurantId = Integer.parseInt(restaurantIdParam.trim());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "restaurantId must be a valid number.");
            return;
        }

        // MenuItemService.getMenuByRestaurantId() returns all items (available + unavailable)
        // We filter to only available items for the customer-facing menu
        List<MenuItem> all = menuItemService.getMenuByRestaurantId(restaurantId);
        List<MenuItem> available = all.stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());

        JsonUtil.sendSuccess(response, "Menu fetched successfully.", available);
    }
}
