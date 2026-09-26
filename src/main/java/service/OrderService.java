package service;

import dao.CartDAO;
import dao.OrderDAO;
import model.Cart;
import model.CartItem;
import model.Order;
import model.OrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();

    public Order checkout(int userId, String deliveryAddress) {
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            System.out.println("Delivery address is required.");
            return null;
        }

        // 1. Get the active cart for the user
        Cart activeCart = cartDAO.getActiveCartByUserId(userId);
        if (activeCart == null) {
            System.out.println("No active cart found for checkout.");
            return null;
        }

        // 2. Validate that the cart is not empty
        List<CartItem> cartItems = cartDAO.getCartItems(activeCart.getCartId());
        if (cartItems == null || cartItems.isEmpty()) {
            System.out.println("Cart is empty. Cannot checkout.");
            return null;
        }

        // 3. Create a new order
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem ci : cartItems) {
            BigDecimal subtotal = ci.getLineTotal();
            totalAmount = totalAmount.add(subtotal);

            // Copy cart items into order_items
            OrderItem orderItem = new OrderItem(
                    0, // orderId will be set in DAO
                    ci.getMenuItemId(),
                    ci.getItemName(),
                    ci.getQuantity(),
                    ci.getUnitPrice(),
                    subtotal
            );
            orderItems.add(orderItem);
        }

        Order newOrder = new Order(userId, activeCart.getRestaurantId(), totalAmount, deliveryAddress);

        // 4. Use DAO to insert order, items, and clear cart in a transaction
        boolean success = orderDAO.createOrderWithTransaction(newOrder, orderItems, activeCart.getCartId());

        if (success) {
            System.out.println("Order created successfully! Order ID: " + newOrder.getOrderId());
            return newOrder;
        } else {
            System.out.println("Failed to create order. Transaction rolled back.");
            return null;
        }
    }

    public Order getOrderById(int orderId) {
        return orderDAO.getOrderById(orderId);
    }

    public List<Order> getOrdersByUserId(int userId) {
        return orderDAO.getOrdersByUserId(userId);
    }

    public List<OrderItem> getOrderItemsByOrderId(int orderId) {
        return orderDAO.getOrderItemsByOrderId(orderId);
    }

    public List<Order> getOrdersByRestaurantId(int userId, int restaurantId) {
        // Validation: Ensure the user actually owns this restaurant
        dao.RestaurantDAO restaurantDAO = new dao.RestaurantDAO();
        model.Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        
        if (restaurant == null || restaurant.getOwnerId() != userId) {
            System.out.println("Access denied. You do not own this restaurant.");
            return null;
        }

        return orderDAO.getOrdersByRestaurantId(restaurantId);
    }

    public boolean updateOrderStatusByRestaurant(int userId, int orderId, int restaurantId, String newStatus) {
        // Validation: Ensure user owns the restaurant
        dao.RestaurantDAO restaurantDAO = new dao.RestaurantDAO();
        model.Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        
        if (restaurant == null || restaurant.getOwnerId() != userId) {
            System.out.println("Access denied. You do not own this restaurant.");
            return false;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || order.getRestaurantId() != restaurantId) {
            System.out.println("Order not found or doesn't belong to your restaurant.");
            return false;
        }

        String currentStatus = order.getStatus();

        // Validate state transitions
        if ("DELIVERED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            System.out.println("Cannot change status. Order is already " + currentStatus + " (Final State).");
            return false;
        }

        boolean validTransition = false;
        switch (newStatus) {
            case "CONFIRMED":
                if ("PLACED".equals(currentStatus)) validTransition = true;
                break;
            case "PREPARING":
                if ("PLACED".equals(currentStatus) || "CONFIRMED".equals(currentStatus)) validTransition = true;
                break;
            case "READY":
                if ("PREPARING".equals(currentStatus)) validTransition = true;
                break;
            case "CANCELLED":
                if ("PLACED".equals(currentStatus) || "CONFIRMED".equals(currentStatus)) validTransition = true;
                break;
        }

        if (!validTransition) {
            System.out.println("Invalid status transition from " + currentStatus + " to " + newStatus);
            return false;
        }

        boolean success = orderDAO.updateOrderStatus(orderId, newStatus);
        if (success) {
            System.out.println("Order status updated successfully to: " + newStatus);
        } else {
            System.out.println("Failed to update order status.");
        }
        return success;
    }
}
