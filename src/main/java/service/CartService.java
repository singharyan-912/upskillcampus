package service;

import dao.CartDAO;
import dao.MenuItemDAO;
import dao.RestaurantDAO;
import model.Cart;
import model.CartItem;
import model.MenuItem;
import model.Restaurant;

import java.math.BigDecimal;
import java.util.List;

public class CartService {

    private final CartDAO cartDAO = new CartDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    /**
     * Adds a menu item to the customer's cart.
     * Creates a new ACTIVE cart if the customer does not have one.
     * If the customer already has an ACTIVE cart from a different restaurant, rejects the request.
     * If the same item already exists in the cart, increments the quantity instead.
     */
    public boolean addItemToCart(int userId, int menuItemId, int quantity) {

        // Validate quantity
        if (quantity < 1) {
            System.out.println("Quantity must be at least 1.");
            return false;
        }

        // Fetch the menu item and verify it exists and is available
        MenuItem menuItem = menuItemDAO.getMenuItemById(menuItemId);
        if (menuItem == null) {
            System.out.println("Menu item not found.");
            return false;
        }
        if (!menuItem.isAvailable()) {
            System.out.println("Menu item is currently unavailable.");
            return false;
        }

        int menuRestaurantId = menuItem.getRestaurantId();

        // Check if the customer already has an active cart
        Cart activeCart = cartDAO.getActiveCartByUserId(userId);

        if (activeCart == null) {
            // No active cart — create one locked to this restaurant
            Restaurant restaurant = restaurantDAO.getRestaurantById(menuRestaurantId);
            if (restaurant == null || !"ACTIVE".equalsIgnoreCase(restaurant.getStatus())) {
                System.out.println("Restaurant is not available.");
                return false;
            }
            Cart newCart = new Cart(userId, menuRestaurantId);
            activeCart = cartDAO.createCart(newCart);
            if (activeCart == null) {
                System.out.println("Failed to create cart.");
                return false;
            }
        } else {
            // Cart exists — enforce single-restaurant policy
            if (activeCart.getRestaurantId() != menuRestaurantId) {
                System.out.println("Your cart contains items from a different restaurant. " +
                        "Please clear your cart before adding items from a new restaurant.");
                return false;
            }
        }

        // Check if this item is already in the cart
        CartItem existing = cartDAO.getCartItemByMenuItemId(activeCart.getCartId(), menuItemId);
        if (existing != null) {
            // Item already in cart — update quantity instead of duplicating
            int newQuantity = existing.getQuantity() + quantity;
            return cartDAO.updateCartItemQuantity(existing.getCartItemId(), newQuantity);
        }

        // New item — take a price snapshot and add to cart
        CartItem newItem = new CartItem(activeCart.getCartId(), menuItemId, quantity, menuItem.getPrice());
        return cartDAO.addCartItem(newItem);
    }

    /**
     * Returns all items in the customer's active cart.
     */
    public List<CartItem> viewCart(int userId) {
        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            System.out.println("No active cart found.");
            return List.of();
        }
        return cartDAO.getCartItems(activeCart.getCartId());
    }

    /**
     * Updates the quantity of a specific item in the customer's cart.
     * The item must belong to the customer's own cart.
     */
    public boolean updateItemQuantity(int userId, int cartItemId, int newQuantity) {

        if (newQuantity < 1) {
            System.out.println("Quantity must be at least 1. Use removeItem to delete an item.");
            return false;
        }

        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            System.out.println("No active cart found.");
            return false;
        }

        // Verify the cart item belongs to this user's cart
        List<CartItem> items = cartDAO.getCartItems(activeCart.getCartId());
        boolean owned = items.stream().anyMatch(i -> i.getCartItemId() == cartItemId);
        if (!owned) {
            System.out.println("Cart item not found in your cart.");
            return false;
        }

        return cartDAO.updateCartItemQuantity(cartItemId, newQuantity);
    }

    /**
     * Removes a specific item from the customer's cart.
     */
    public boolean removeItem(int userId, int cartItemId) {

        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            System.out.println("No active cart found.");
            return false;
        }

        List<CartItem> items = cartDAO.getCartItems(activeCart.getCartId());
        boolean owned = items.stream().anyMatch(i -> i.getCartItemId() == cartItemId);
        if (!owned) {
            System.out.println("Cart item not found in your cart.");
            return false;
        }

        return cartDAO.removeCartItem(cartItemId);
    }

    /**
     * Calculates the total price of all items in the customer's active cart.
     */
    public BigDecimal getCartTotal(int userId) {
        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            return BigDecimal.ZERO;
        }
        return cartDAO.getCartTotal(activeCart.getCartId());
    }

    /**
     * Clears all items from the active cart and marks it as ORDERED.
     * Called when a customer places an order.
     */
    public boolean clearCart(int userId) {
        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            System.out.println("No active cart to clear.");
            return false;
        }
        cartDAO.clearCartItems(activeCart.getCartId());
        return cartDAO.updateCartStatus(activeCart.getCartId(), "ORDERED");
    }

    /**
     * Returns the customer's active Cart object.
     */
    public Cart getActiveCart(int userId) {
        return cartDAO.getActiveCartByUserId(userId);
    }
}
