package controller;

import model.Restaurant;
import service.RestaurantService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RestaurantServlet — exposes restaurant data to the frontend.
 *
 * Endpoints:
 *   GET /api/restaurants          → list all ACTIVE restaurants
 *   GET /api/restaurants/{id}     → get a single restaurant by ID
 *
 * Responsibility: Read request → call RestaurantService → return JSON.
 */
@WebServlet("/api/restaurants/*")
public class RestaurantServlet extends HttpServlet {

    private final RestaurantService restaurantService = new RestaurantService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo(); // null or "/{id}"

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/restaurants — return all ACTIVE restaurants
            List<Restaurant> all = restaurantService.getAllRestaurants();

            // Filter to only ACTIVE restaurants for the customer-facing view
            List<Restaurant> active = all.stream()
                    .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());

            JsonUtil.sendSuccess(response, "Restaurants fetched successfully.", active);

        } else {
            // GET /api/restaurants/{id}
            // pathInfo is like "/42" — strip leading slash
            try {
                int restaurantId = Integer.parseInt(pathInfo.substring(1));
                Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);

                if (restaurant == null) {
                    JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Restaurant not found.");
                } else {
                    JsonUtil.sendSuccess(response, "Restaurant fetched.", restaurant);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid restaurant ID.");
            }
        }
    }
}
