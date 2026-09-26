package main;

import config.DatabaseConnection;
import model.User;
import model.Restaurant;
import model.MenuItem;
import model.CartItem;
import model.Order;
import model.OrderItem;
import model.Payment;
import model.Delivery;
import service.UserService;
import service.RestaurantService;
import service.MenuItemService;
import service.CartService;
import service.OrderService;
import service.PaymentService;
import service.DeliveryService;
import service.ReviewService;
import service.AdminService;
import service.PaymentService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class main {

    public static void main(String[] args) {

        UserService userService = new UserService();
        RestaurantService restaurantService = new RestaurantService();
        MenuItemService menuItemService = new MenuItemService();

        // 1. Database cleanup of test accounts and restaurants
        String[] testEmails = {
                "customer@example.com",
                "restaurant@example.com",
                "delivery@example.com",
                "admin@example.com"
        };

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement deleteRestaurants = connection.prepareStatement("DELETE FROM restaurants");
                PreparedStatement deleteUsers = connection.prepareStatement("DELETE FROM users WHERE email = ?")
        ) {
            // Delete restaurants first to avoid foreign key issues (even though ON DELETE CASCADE is set)
            deleteRestaurants.executeUpdate();

            for (String email : testEmails) {
                deleteUsers.setString(1, email);
                deleteUsers.executeUpdate();
            }
            System.out.println("Cleaned up database for testing.");
        } catch (SQLException e) {
            System.out.println("Cleanup failed: " + e.getMessage());
        }

        // 2. Register users with different roles
        System.out.println("\n--- REGISTERING USERS FOR EACH ROLE ---");
        
        User customer = new User("Alice Customer", "customer@example.com", "pass123", "1111111111", "CUSTOMER");
        User restaurantOwner = new User("Tasty Bites Owner", "restaurant@example.com", "pass123", "2222222222", "RESTAURANT");
        User delivery = new User("Bob Rider", "delivery@example.com", "pass123", "3333333333", "DELIVERY_PARTNER");
        User admin = new User("Super Admin", "admin@example.com", "pass123", "4444444444", "ADMIN");

        System.out.println("Customer Registration: " + (userService.registerUser(customer) ? "SUCCESS" : "FAILED"));
        System.out.println("Restaurant Owner Registration: " + (userService.registerUser(restaurantOwner) ? "SUCCESS" : "FAILED"));
        System.out.println("Delivery Partner Registration: " + (userService.registerUser(delivery) ? "SUCCESS" : "FAILED"));
        System.out.println("Admin Registration: " + (userService.registerUser(admin) ? "SUCCESS" : "FAILED"));

        // Fetch users from DB to retrieve generated user IDs
        User dbCustomer = userService.loginUser("customer@example.com", "pass123");
        User dbRestaurantOwner = userService.loginUser("restaurant@example.com", "pass123");

        System.out.println("\n--- RUNNING RESTAURANT MODULE VALIDATION TESTS ---");

        // Test Case R1: Create restaurant with customer role owner
        System.out.println("\nTest Case R1: Create restaurant owned by a CUSTOMER user");
        Restaurant r1 = new Restaurant(dbCustomer.getUserId(), "Pizza Hut", "123 Main St", "555-0199", "Italian", "ACTIVE");
        boolean r1Result = restaurantService.createRestaurant(r1);
        System.out.println("Expected: Fails, Actual: " + (r1Result ? "SUCCESS" : "FAILED"));

        // Test Case R2: Create restaurant with non-existent owner
        System.out.println("\nTest Case R2: Create restaurant with non-existent owner ID (9999)");
        Restaurant r2 = new Restaurant(9999, "Subway", "456 Oak St", "555-0200", "Fast Food", "ACTIVE");
        boolean r2Result = restaurantService.createRestaurant(r2);
        System.out.println("Expected: Fails, Actual: " + (r2Result ? "SUCCESS" : "FAILED"));

        // Test Case R3: Create restaurant with empty name
        System.out.println("\nTest Case R3: Create restaurant with empty name");
        Restaurant r3 = new Restaurant(dbRestaurantOwner.getUserId(), "", "789 Pine St", "555-0300", "Mexican", "ACTIVE");
        boolean r3Result = restaurantService.createRestaurant(r3);
        System.out.println("Expected: Fails, Actual: " + (r3Result ? "SUCCESS" : "FAILED"));

        // Test Case R4: Create valid restaurant
        System.out.println("\nTest Case R4: Create valid restaurant with RESTAURANT owner");
        Restaurant validRestaurant = new Restaurant(dbRestaurantOwner.getUserId(), "Gourmet Garden", "101 Flower Ave", "555-1234", "Healthy", "ACTIVE");
        boolean validResult = restaurantService.createRestaurant(validRestaurant);
        System.out.println("Expected: Success, Actual: " + (validResult ? "SUCCESS" : "FAILED"));

        // Test Case R5: List all restaurants and retrieve by owner
        System.out.println("\nTest Case R5: Retrieve restaurants from Database");
        List<Restaurant> allRestaurants = restaurantService.getAllRestaurants();
        System.out.println("Total Restaurants registered: " + allRestaurants.size());
        for (Restaurant r : allRestaurants) {
            System.out.println("-> ID: " + r.getRestaurantId() + ", Name: " + r.getName() + 
                               ", Cuisine: " + r.getCuisineType() + ", Owner ID: " + r.getOwnerId() + 
                               ", Status: " + r.getStatus() + ", Created: " + r.getCreatedAt());
        }

        // Test Case R6: Update Restaurant details
        System.out.println("\nTest Case R6: Update Restaurant details");
        if (!allRestaurants.isEmpty()) {
            Restaurant addedRestaurant = allRestaurants.get(0);
            addedRestaurant.setName("Gourmet Garden Bistro");
            addedRestaurant.setCuisineType("Gourmet Healthy");
            addedRestaurant.setAddress("202 Garden Boulevard");
            
            boolean updateResult = restaurantService.updateRestaurant(addedRestaurant);
            System.out.println("Expected: Success, Actual: " + (updateResult ? "SUCCESS" : "FAILED"));

            // Retrieve again to verify
            Restaurant updated = restaurantService.getRestaurantById(addedRestaurant.getRestaurantId());
            System.out.println("Updated Name: " + updated.getName());
            System.out.println("Updated Cuisine: " + updated.getCuisineType());
            System.out.println("Updated Address: " + updated.getAddress());
        } else {
            System.out.println("Failed to retrieve any restaurant for update test.");
        }

        // ---- MENU ITEM MODULE TESTS ----
        System.out.println("\n\n========================================");
        System.out.println("       MENU ITEM MODULE TESTS");
        System.out.println("========================================");

        // Fetch fresh restaurant list and owner details for menu tests
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        if (restaurants.isEmpty()) {
            System.out.println("No restaurants found. Skipping menu tests.");
            return;
        }

        Restaurant testRestaurant = restaurants.get(0);
        int ownerId = testRestaurant.getOwnerId();
        int restaurantId = testRestaurant.getRestaurantId();

        // Fetch a non-owner (customer) ID for access-denial tests
        User customerUser = userService.loginUser("customer@example.com", "pass123");
        int nonOwnerId = (customerUser != null) ? customerUser.getUserId() : -1;

        // ---- Test M1: Add menu item with zero price ----
        System.out.println("\nTest M1: Add menu item with price = 0 (should fail)");
        MenuItem invalidItem = new MenuItem(restaurantId, "Invalid Burger", "No price", BigDecimal.ZERO, "Burger", true);
        System.out.println("Result: " + (menuItemService.addMenuItem(invalidItem, ownerId) ? "SUCCESS" : "FAILED (Expected)"));

        // ---- Test M2: Add item by non-owner (should fail) ----
        System.out.println("\nTest M2: Add item by non-owner CUSTOMER (should fail)");
        MenuItem itemByNonOwner = new MenuItem(restaurantId, "Pasta", "Creamy pasta", new BigDecimal("199.00"), "Main Course", true);
        System.out.println("Result: " + (menuItemService.addMenuItem(itemByNonOwner, nonOwnerId) ? "SUCCESS" : "FAILED (Expected)"));

        // ---- Test M3: Add valid menu item by restaurant owner ----
        System.out.println("\nTest M3: Add valid menu item by owner");
        MenuItem item1 = new MenuItem(restaurantId, "Garden Salad", "Fresh seasonal greens", new BigDecimal("149.00"), "Starters", true);
        MenuItem item2 = new MenuItem(restaurantId, "Grilled Chicken", "Herb-marinated chicken", new BigDecimal("349.00"), "Main Course", true);
        MenuItem item3 = new MenuItem(restaurantId, "Mango Smoothie", "Cold mango drink", new BigDecimal("99.00"), "Beverages", true);
        System.out.println("Garden Salad Added: " + (menuItemService.addMenuItem(item1, ownerId) ? "SUCCESS" : "FAILED"));
        System.out.println("Grilled Chicken Added: " + (menuItemService.addMenuItem(item2, ownerId) ? "SUCCESS" : "FAILED"));
        System.out.println("Mango Smoothie Added: " + (menuItemService.addMenuItem(item3, ownerId) ? "SUCCESS" : "FAILED"));

        // ---- Test M4: View all menu items by restaurant ----
        System.out.println("\nTest M4: View all menu items for restaurant ID " + restaurantId);
        List<MenuItem> menu = menuItemService.getMenuByRestaurantId(restaurantId);
        System.out.println("Total Items: " + menu.size());
        for (MenuItem item : menu) {
            System.out.println("  -> [" + item.getMenuItemId() + "] " + item.getItemName() +
                    " | Rs." + item.getPrice() + " | " + item.getCategory() +
                    " | Available: " + item.isAvailable());
        }

        // ---- Test M5: Update a menu item ----
        System.out.println("\nTest M5: Update first menu item");
        if (!menu.isEmpty()) {
            MenuItem toUpdate = menu.get(0);
            toUpdate.setItemName("Garden Salad (Special)");
            toUpdate.setPrice(new BigDecimal("179.00"));
            toUpdate.setDescription("Fresh greens with seasonal toppings");
            boolean updated = menuItemService.updateMenuItem(toUpdate, ownerId);
            System.out.println("Update Result: " + (updated ? "SUCCESS" : "FAILED"));
        }

        // ---- Test M6: Toggle availability off ----
        System.out.println("\nTest M6: Mark Grilled Chicken as unavailable");
        if (menu.size() >= 2) {
            int chickenId = menu.get(1).getMenuItemId();
            System.out.println("Availability Update: " +
                    (menuItemService.updateAvailability(chickenId, false, ownerId) ? "SUCCESS" : "FAILED"));
        }

        // ---- Test M7: Delete by non-owner (should fail) ----
        System.out.println("\nTest M7: Delete item by non-owner (should fail)");
        if (!menu.isEmpty()) {
            int itemId = menu.get(0).getMenuItemId();
            System.out.println("Result: " + (menuItemService.deleteMenuItem(itemId, nonOwnerId) ? "SUCCESS" : "FAILED (Expected)"));
        }

        // ---- Test M8: Delete menu item by owner ----
        System.out.println("\nTest M8: Delete last menu item by owner");
        if (menu.size() >= 3) {
            int lastItemId = menu.get(2).getMenuItemId();
            System.out.println("Delete Result: " + (menuItemService.deleteMenuItem(lastItemId, ownerId) ? "SUCCESS" : "FAILED"));
        }

        // ---- Final: View menu after changes ----
        System.out.println("\nFinal Menu State after all operations:");
        List<MenuItem> finalMenu = menuItemService.getMenuByRestaurantId(restaurantId);
        System.out.println("Remaining Items: " + finalMenu.size());
        for (MenuItem item : finalMenu) {
            System.out.println("  -> [" + item.getMenuItemId() + "] " + item.getItemName() +
                    " | Rs." + item.getPrice() + " | " + item.getCategory() +
                    " | Available: " + item.isAvailable());
        }

        // ════════════════════════════════════════
        //           CART MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           CART MODULE TESTS");
        System.out.println("========================================");

        CartService cartService = new CartService();

        // Get the customer and restaurant from already-registered test data
        User cartCustomer = userService.loginUser("customer@example.com", "pass123");
        int cartUserId = cartCustomer.getUserId();

        // Get menu items from the final menu to use as test items
        List<MenuItem> cartMenu = menuItemService.getMenuByRestaurantId(restaurantId);
        if (cartMenu.size() < 2) {
            System.out.println("Not enough menu items for cart tests.");
            return;
        }
        int item1Id = cartMenu.get(0).getMenuItemId(); // Garden Salad (Special)
        int item2Id = cartMenu.get(1).getMenuItemId(); // Grilled Chicken

        // ── Test C1: Add item with invalid quantity ──
        System.out.println("\nTest C1: Add item with quantity = 0 (should fail)");
        System.out.println("Result: " + (cartService.addItemToCart(cartUserId, item1Id, 0) ? "SUCCESS" : "FAILED (Expected)"));

        // ── Test C2: Add first item to cart ──
        System.out.println("\nTest C2: Add Garden Salad x2 to cart");
        System.out.println("Result: " + (cartService.addItemToCart(cartUserId, item1Id, 2) ? "SUCCESS" : "FAILED"));

        // ── Test C3: Add same item again (should increment quantity, not duplicate) ──
        System.out.println("\nTest C3: Add Garden Salad x1 again (should increment to 3)");
        System.out.println("Result: " + (cartService.addItemToCart(cartUserId, item1Id, 1) ? "SUCCESS" : "FAILED"));

        // ── Test C4: Add second item from same restaurant ──
        System.out.println("\nTest C4: Add Grilled Chicken x1 to cart");
        System.out.println("Result: " + (cartService.addItemToCart(cartUserId, item2Id, 1) ? "SUCCESS" : "FAILED"));

        // ── Test C5: View cart ──
        System.out.println("\nTest C5: View cart contents");
        List<CartItem> cartItems = cartService.viewCart(cartUserId);
        System.out.println("Items in cart: " + cartItems.size());
        for (CartItem ci : cartItems) {
            System.out.println("  -> [" + ci.getCartItemId() + "] " + ci.getItemName() +
                    " | Qty: " + ci.getQuantity() +
                    " | Unit: Rs." + ci.getUnitPrice() +
                    " | Line Total: Rs." + ci.getLineTotal());
        }

        // ── Test C6: Get cart total ──
        System.out.println("\nTest C6: Calculate cart total");
        System.out.println("Cart Total: Rs." + cartService.getCartTotal(cartUserId));

        // ── Test C7: Update quantity of first item ──
        System.out.println("\nTest C7: Update Garden Salad quantity to 1");
        if (!cartItems.isEmpty()) {
            int cartItemId = cartItems.get(0).getCartItemId();
            System.out.println("Result: " + (cartService.updateItemQuantity(cartUserId, cartItemId, 1) ? "SUCCESS" : "FAILED"));
            System.out.println("Updated Cart Total: Rs." + cartService.getCartTotal(cartUserId));
        }

        // ── Test C8: Remove an item ──
        System.out.println("\nTest C8: Remove Grilled Chicken from cart");
        if (cartItems.size() >= 2) {
            int cartItemId = cartItems.get(1).getCartItemId();
            System.out.println("Result: " + (cartService.removeItem(cartUserId, cartItemId) ? "SUCCESS" : "FAILED"));
            System.out.println("Cart Total after removal: Rs." + cartService.getCartTotal(cartUserId));
        }

        // ── Test C9: Clear cart (simulate order placement) ──
        System.out.println("\nTest C9: Clear cart (simulating order placed)");
        System.out.println("Result: " + (cartService.clearCart(cartUserId) ? "SUCCESS" : "FAILED"));

        // ── Test C10: Verify cart is gone after clearing ──
        System.out.println("\nTest C10: View cart after clearing (should be empty)");
        List<CartItem> clearedCart = cartService.viewCart(cartUserId);
        System.out.println("Items remaining: " + clearedCart.size());
        System.out.println("Items remaining: " + clearedCart.size());

        // ════════════════════════════════════════
        //           ORDER MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           ORDER MODULE TESTS");
        System.out.println("========================================");

        OrderService orderService = new OrderService();

        // Add items to cart again for order checkout test
        System.out.println("\n--- Preparing Cart for Order Checkout ---");
        cartService.addItemToCart(cartUserId, item1Id, 2); // 2 Garden Salads
        System.out.println("Cart prepped with 2 Garden Salads.");

        // ── Test O1: Checkout empty cart ──
        // (First clear it to test empty checkout)
        cartService.clearCart(cartUserId);
        System.out.println("\nTest O1: Checkout empty cart (should fail)");
        System.out.println("Result: " + (orderService.checkout(cartUserId, "123 Test St") != null ? "SUCCESS" : "FAILED (Expected)"));

        // Add items back
        cartService.addItemToCart(cartUserId, item1Id, 2);

        // ── Test O2: Checkout with missing address ──
        System.out.println("\nTest O2: Checkout with missing address (should fail)");
        System.out.println("Result: " + (orderService.checkout(cartUserId, "") != null ? "SUCCESS" : "FAILED (Expected)"));

        // ── Test O3: Successful checkout ──
        System.out.println("\nTest O3: Checkout cart successfully");
        Order createdOrder = orderService.checkout(cartUserId, "123 Main St, Springfield");
        System.out.println("Result: " + (createdOrder != null ? "SUCCESS" : "FAILED"));

        if (createdOrder != null) {
            // ── Test O4: Verify Order Details ──
            System.out.println("\nTest O4: Verify created order details");
            Order dbOrder = orderService.getOrderById(createdOrder.getOrderId());
            System.out.println("Order ID: " + dbOrder.getOrderId());
            System.out.println("Total Amount: Rs." + dbOrder.getTotalAmount());
            System.out.println("Status: " + dbOrder.getStatus());
            System.out.println("Address: " + dbOrder.getDeliveryAddress());

            // ── Test O5: Verify Order Items ──
            System.out.println("\nTest O5: Verify order items");
            List<OrderItem> orderItems = orderService.getOrderItemsByOrderId(createdOrder.getOrderId());
            System.out.println("Order items count: " + orderItems.size());
            for (OrderItem oi : orderItems) {
                System.out.println("  -> " + oi.getItemName() + " | Qty: " + oi.getQuantity() +
                        " | Unit: Rs." + oi.getUnitPrice() + " | Subtotal: Rs." + oi.getSubtotal());
            }

            // ── Test O6: Verify Cart is empty after successful order ──
            System.out.println("\nTest O6: Verify cart is cleared after order");
            List<CartItem> postOrderCart = cartService.viewCart(cartUserId);
            System.out.println("Items in cart: " + postOrderCart.size() + " (Expected 0)");
        }
        
        // ── Test O7: View all orders for customer ──
        System.out.println("\nTest O7: View all orders for customer");
        List<Order> customerOrders = orderService.getOrdersByUserId(cartUserId);
        System.out.println("Total Orders: " + customerOrders.size());
        for (Order o : customerOrders) {
            System.out.println("  -> Order ID: " + o.getOrderId() + " | Total: Rs." + o.getTotalAmount() + " | Status: " + o.getStatus());
        }

        // ════════════════════════════════════════
        //           PAYMENT MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           PAYMENT MODULE TESTS");
        System.out.println("========================================");

        PaymentService paymentService = new PaymentService();

        if (createdOrder != null) {
            int orderId = createdOrder.getOrderId();
            
            // ── Test P1: Invalid Payment Method ──
            System.out.println("\nTest P1: Invalid payment method (should fail)");
            Payment invalidPayment = paymentService.processPayment(cartUserId, orderId, "CRYPTO", true);
            System.out.println("Result: " + (invalidPayment == null ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test P2: Online Payment (Simulate Failure) ──
            System.out.println("\nTest P2: Online Payment simulating failure");
            Payment failedPayment = paymentService.processPayment(cartUserId, orderId, "ONLINE", false);
            System.out.println("Result: " + (failedPayment != null && "FAILED".equals(failedPayment.getPaymentStatus()) ? "SUCCESS" : "FAILED"));

            // ── Test P3: Online Payment (Simulate Success) ──
            System.out.println("\nTest P3: Online Payment simulating success");
            Payment successPayment = paymentService.processPayment(cartUserId, orderId, "ONLINE", true);
            System.out.println("Result: " + (successPayment != null && "SUCCESS".equals(successPayment.getPaymentStatus()) ? "SUCCESS" : "FAILED"));

            // ── Test P4: Verify Order Status updated to CONFIRMED ──
            System.out.println("\nTest P4: Verify order status is CONFIRMED");
            Order verifiedOrder = orderService.getOrderById(orderId);
            System.out.println("Order Status: " + verifiedOrder.getStatus());
            System.out.println("Result: " + ("CONFIRMED".equals(verifiedOrder.getStatus()) ? "SUCCESS" : "FAILED"));

            // ── Test P5: Reject another SUCCESS payment for same order ──
            System.out.println("\nTest P5: Try paying again for same order (should fail)");
            Payment duplicatePayment = paymentService.processPayment(cartUserId, orderId, "COD", true);
            System.out.println("Result: " + (duplicatePayment == null ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test P6: View Payments for Order ──
            System.out.println("\nTest P6: View Payments for order");
            List<Payment> payments = paymentService.getPaymentsByOrderId(cartUserId, orderId);
            System.out.println("Total Payment Attempts: " + payments.size());
            for (Payment p : payments) {
                System.out.println("  -> ID: " + p.getPaymentId() + " | Method: " + p.getPaymentMethod() + " | Status: " + p.getPaymentStatus() + " | Ref: " + p.getTransactionRef());
            }
        }

        // ════════════════════════════════════════
        //           DELIVERY MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           DELIVERY MODULE TESTS");
        System.out.println("========================================");

        DeliveryService deliveryService = new DeliveryService();
        User dbDelivery = userService.loginUser("delivery@example.com", "pass123");
        User dbRestaurant = userService.loginUser("restaurant@example.com", "pass123");
        int deliveryUserId = dbDelivery.getUserId();
        int restaurantOwnerId = dbRestaurant.getUserId();
        
        if (createdOrder != null) {
            int orderId = createdOrder.getOrderId();
            int orderRestaurantId = createdOrder.getRestaurantId();

            // ── Test D1: Assign Delivery before READY (should fail) ──
            System.out.println("\nTest D1: Assign delivery before READY");
            System.out.println("Result: " + (deliveryService.assignDelivery(deliveryUserId, orderId) ? "SUCCESS" : "FAILED (Expected)"));

            // Setup: Restaurant updates status to PREPARING then READY
            System.out.println("\n--- Restaurant processing order ---");
            orderService.updateOrderStatusByRestaurant(restaurantOwnerId, orderId, orderRestaurantId, "PREPARING");
            orderService.updateOrderStatusByRestaurant(restaurantOwnerId, orderId, orderRestaurantId, "READY");

            // ── Test D2: Assign Delivery after READY ──
            System.out.println("\nTest D2: Assign delivery after READY");
            System.out.println("Result: " + (deliveryService.assignDelivery(deliveryUserId, orderId) ? "SUCCESS" : "FAILED"));

            // ── Test D3: Invalid status transition (ASSIGNED -> DELIVERED) ──
            System.out.println("\nTest D3: Invalid transition ASSIGNED -> DELIVERED");
            System.out.println("Result: " + (deliveryService.updateDeliveryStatus(deliveryUserId, orderId, "DELIVERED") ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test D4: Update to PICKED_UP ──
            System.out.println("\nTest D4: Update to PICKED_UP");
            System.out.println("Result: " + (deliveryService.updateDeliveryStatus(deliveryUserId, orderId, "PICKED_UP") ? "SUCCESS" : "FAILED"));

            // ── Test D5: Update to OUT_FOR_DELIVERY ──
            System.out.println("\nTest D5: Update to OUT_FOR_DELIVERY");
            System.out.println("Result: " + (deliveryService.updateDeliveryStatus(deliveryUserId, orderId, "OUT_FOR_DELIVERY") ? "SUCCESS" : "FAILED"));

            // ── Test D6: Update to DELIVERED ──
            System.out.println("\nTest D6: Update to DELIVERED (Final)");
            System.out.println("Result: " + (deliveryService.updateDeliveryStatus(deliveryUserId, orderId, "DELIVERED") ? "SUCCESS" : "FAILED"));

            // ── Test D7: Verify Order Status updated to DELIVERED ──
            System.out.println("\nTest D7: Verify order status synced to DELIVERED");
            Order finalOrder = orderService.getOrderById(orderId);
            System.out.println("Order Status: " + finalOrder.getStatus());
            System.out.println("Result: " + ("DELIVERED".equals(finalOrder.getStatus()) ? "SUCCESS" : "FAILED"));

            // ── Test D8: Try updating DELIVERED order back to PREPARING (should fail) ──
            System.out.println("\nTest D8: Restaurant tries to change DELIVERED order back to PREPARING");
            System.out.println("Result: " + (orderService.updateOrderStatusByRestaurant(restaurantOwnerId, orderId, orderRestaurantId, "PREPARING") ? "SUCCESS" : "FAILED (Expected)"));
        }
        
        // ════════════════════════════════════════
        //           REVIEW MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           REVIEW MODULE TESTS");
        System.out.println("========================================");

        ReviewService reviewService = new ReviewService();
        
        if (createdOrder != null) {
            int revOrderId = createdOrder.getOrderId();
            int revRestaurantId = createdOrder.getRestaurantId();

            // ── Test RV1: Invalid rating (6) ──
            System.out.println("\nTest RV1: Submit review with invalid rating (6)");
            System.out.println("Result: " + (reviewService.submitReview(cartUserId, revOrderId, 6, "Great!") ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test RV2: Review by non-owner ──
            System.out.println("\nTest RV2: Review by someone who didn't place the order");
            System.out.println("Result: " + (reviewService.submitReview(restaurantOwnerId, revOrderId, 5, "Good") ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test RV3: Submit valid review ──
            System.out.println("\nTest RV3: Submit valid review (5 stars)");
            System.out.println("Result: " + (reviewService.submitReview(cartUserId, revOrderId, 5, "Excellent food and fast delivery!") ? "SUCCESS" : "FAILED"));

            // ── Test RV4: Submit duplicate review ──
            System.out.println("\nTest RV4: Attempt to submit second review for same order");
            System.out.println("Result: " + (reviewService.submitReview(cartUserId, revOrderId, 4, "Another review") ? "SUCCESS" : "FAILED (Expected)"));

            // ── Test RV5: View reviews for restaurant ──
            System.out.println("\nTest RV5: View reviews for restaurant");
            List<model.Review> reviews = reviewService.getReviewsForRestaurant(revRestaurantId);
            System.out.println("Total Reviews: " + reviews.size());
            for (model.Review r : reviews) {
                System.out.println("  -> Rating: " + r.getRating() + "/5 | Comment: " + r.getReviewComment());
            }

            // ── Test RV6: View average rating ──
            System.out.println("\nTest RV6: View average rating for restaurant");
            System.out.println("Average Rating: " + reviewService.getAverageRating(revRestaurantId));
        }
        
        // ════════════════════════════════════════
        //           ADMIN MODULE TESTS
        // ════════════════════════════════════════
        System.out.println("\n\n========================================");
        System.out.println("           ADMIN MODULE TESTS");
        System.out.println("========================================");

        AdminService adminService = new AdminService();
        User dbAdmin = userService.loginUser("admin@example.com", "pass123");
        
        // ── Test A1: View statistics as Customer (should fail) ──
        System.out.println("\nTest A1: Customer tries to view stats");
        adminService.printSystemStatistics(cartUserId);

        // ── Test A2: View statistics as Admin ──
        System.out.println("\nTest A2: Admin views stats");
        adminService.printSystemStatistics(dbAdmin.getUserId());
    }
}