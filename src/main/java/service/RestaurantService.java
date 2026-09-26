package service;

import dao.RestaurantDAO;
import dao.UserDAO;
import model.Restaurant;
import model.User;

import java.util.List;

public class RestaurantService {

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final UserDAO userDAO = new UserDAO();

    public boolean createRestaurant(Restaurant restaurant) {

        // Validate basic inputs
        if (restaurant.getName() == null || restaurant.getName().isBlank()) {
            System.out.println("Restaurant name cannot be empty.");
            return false;
        }

        if (restaurant.getAddress() == null || restaurant.getAddress().isBlank()) {
            System.out.println("Restaurant address cannot be empty.");
            return false;
        }

        if (restaurant.getPhone() == null || restaurant.getPhone().isBlank()) {
            System.out.println("Restaurant phone cannot be empty.");
            return false;
        }

        if (restaurant.getCuisineType() == null || restaurant.getCuisineType().isBlank()) {
            System.out.println("Cuisine type cannot be empty.");
            return false;
        }

        // Validate owner existence
        User owner = userDAO.getUserById(restaurant.getOwnerId());
        if (owner == null) {
            System.out.println("Owner not found.");
            return false;
        }

        // Validate owner's role
        if (!"RESTAURANT".equalsIgnoreCase(owner.getRole())) {
            System.out.println("Owner must have the RESTAURANT role.");
            return false;
        }

        // Set default status if empty
        if (restaurant.getStatus() == null || restaurant.getStatus().isBlank()) {
            restaurant.setStatus("ACTIVE");
        }

        return restaurantDAO.createRestaurant(restaurant);
    }

    public Restaurant getRestaurantById(int restaurantId) {
        return restaurantDAO.getRestaurantById(restaurantId);
    }

    public List<Restaurant> getRestaurantsByOwnerId(int ownerId) {
        return restaurantDAO.getRestaurantsByOwnerId(ownerId);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantDAO.getAllRestaurants();
    }

    public boolean updateRestaurant(Restaurant restaurant) {
        // Validate existence
        Restaurant existing = restaurantDAO.getRestaurantById(restaurant.getRestaurantId());
        if (existing == null) {
            System.out.println("Restaurant not found.");
            return false;
        }

        // Validate basic inputs
        if (restaurant.getName() == null || restaurant.getName().isBlank()) {
            System.out.println("Restaurant name cannot be empty.");
            return false;
        }

        if (restaurant.getAddress() == null || restaurant.getAddress().isBlank()) {
            System.out.println("Restaurant address cannot be empty.");
            return false;
        }

        if (restaurant.getPhone() == null || restaurant.getPhone().isBlank()) {
            System.out.println("Restaurant phone cannot be empty.");
            return false;
        }

        if (restaurant.getCuisineType() == null || restaurant.getCuisineType().isBlank()) {
            System.out.println("Cuisine type cannot be empty.");
            return false;
        }

        if (restaurant.getStatus() == null || restaurant.getStatus().isBlank()) {
            System.out.println("Restaurant status cannot be empty.");
            return false;
        }

        return restaurantDAO.updateRestaurant(restaurant);
    }
}
