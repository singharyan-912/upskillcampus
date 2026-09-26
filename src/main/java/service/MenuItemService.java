package service;

import dao.MenuItemDAO;
import dao.RestaurantDAO;
import model.MenuItem;
import model.Restaurant;

import java.math.BigDecimal;
import java.util.List;

public class MenuItemService {

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    public boolean addMenuItem(MenuItem item, int requestingOwnerId) {

        // Verify the restaurant exists
        Restaurant restaurant = restaurantDAO.getRestaurantById(item.getRestaurantId());
        if (restaurant == null) {
            System.out.println("Restaurant not found.");
            return false;
        }

        // Enforce ownership: only the restaurant owner can add items
        if (restaurant.getOwnerId() != requestingOwnerId) {
            System.out.println("Access denied. You do not own this restaurant.");
            return false;
        }

        // Validate item name
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            System.out.println("Item name cannot be empty.");
            return false;
        }

        // Validate category
        if (item.getCategory() == null || item.getCategory().isBlank()) {
            System.out.println("Category cannot be empty.");
            return false;
        }

        // Validate price: must be present and greater than zero
        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Price must be greater than zero.");
            return false;
        }

        return menuItemDAO.addMenuItem(item);
    }

    public List<MenuItem> getMenuByRestaurantId(int restaurantId) {
        return menuItemDAO.getMenuItemsByRestaurantId(restaurantId);
    }

    public boolean updateMenuItem(MenuItem item, int requestingOwnerId) {

        // Verify item exists
        MenuItem existing = menuItemDAO.getMenuItemById(item.getMenuItemId());
        if (existing == null) {
            System.out.println("Menu item not found.");
            return false;
        }

        // Verify the restaurant that owns this item
        Restaurant restaurant = restaurantDAO.getRestaurantById(existing.getRestaurantId());
        if (restaurant == null || restaurant.getOwnerId() != requestingOwnerId) {
            System.out.println("Access denied. You do not own this menu item.");
            return false;
        }

        // Validate item name
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            System.out.println("Item name cannot be empty.");
            return false;
        }

        // Validate category
        if (item.getCategory() == null || item.getCategory().isBlank()) {
            System.out.println("Category cannot be empty.");
            return false;
        }

        // Validate price
        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Price must be greater than zero.");
            return false;
        }

        return menuItemDAO.updateMenuItem(item);
    }

    public boolean updateAvailability(int menuItemId, boolean available, int requestingOwnerId) {

        // Verify item exists
        MenuItem existing = menuItemDAO.getMenuItemById(menuItemId);
        if (existing == null) {
            System.out.println("Menu item not found.");
            return false;
        }

        // Enforce ownership
        Restaurant restaurant = restaurantDAO.getRestaurantById(existing.getRestaurantId());
        if (restaurant == null || restaurant.getOwnerId() != requestingOwnerId) {
            System.out.println("Access denied. You do not own this menu item.");
            return false;
        }

        return menuItemDAO.updateAvailability(menuItemId, available);
    }

    public boolean deleteMenuItem(int menuItemId, int requestingOwnerId) {

        // Verify item exists
        MenuItem existing = menuItemDAO.getMenuItemById(menuItemId);
        if (existing == null) {
            System.out.println("Menu item not found.");
            return false;
        }

        // Enforce ownership
        Restaurant restaurant = restaurantDAO.getRestaurantById(existing.getRestaurantId());
        if (restaurant == null || restaurant.getOwnerId() != requestingOwnerId) {
            System.out.println("Access denied. You do not own this menu item.");
            return false;
        }

        return menuItemDAO.deleteMenuItem(menuItemId);
    }
}
