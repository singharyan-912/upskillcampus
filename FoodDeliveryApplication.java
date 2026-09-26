/**
 * ============================================================================
 * Project Title    : Online Food Delivery Application
 * Author           : Aryan Singh
 * Internship       : Core Java Internship - Upskill Campus
 * Repository       : https://github.com/singharyan-912/upskillcampus
 * Code File Link   : https://github.com/singharyan-912/upskillcampus/blob/main/FoodDeliveryApplication.java
 * ============================================================================
 * 
 * DESCRIPTION:
 * A comprehensive Core Java application for an end-to-end Online Food Delivery System.
 * Implements multi-role authentication, restaurant catalog management, menu items,
 * cart operations, order processing, payment simulation, delivery tracking lifecycle,
 * customer feedback reviews, and administrator system analytics.
 *
 * Designed as a standalone, self-contained implementation with zero external
 * dependencies for immediate compilation and execution:
 *   Compile: javac FoodDeliveryApplication.java
 *   Run    : java FoodDeliveryApplication
 * ============================================================================
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FoodDeliveryApplication {

    // ========================================================================
    // 1. DATA MODELS
    // ========================================================================

    public static class User {
        private int userId;
        private String name;
        private String email;
        private String password;
        private String phone;
        private String role; // CUSTOMER, RESTAURANT, DELIVERY_PARTNER, ADMIN

        public User(int userId, String name, String email, String password, String phone, String role) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.password = password;
            this.phone = phone;
            this.role = role;
        }

        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }

        @Override
        public String toString() {
            return String.format("[ID: %d] %s (%s) | Role: %s | Phone: %s", userId, name, email, role, phone);
        }
    }

    public static class Restaurant {
        private int restaurantId;
        private int ownerId;
        private String name;
        private String address;
        private String phone;
        private String cuisineType;
        private String status; // ACTIVE, INACTIVE

        public Restaurant(int restaurantId, int ownerId, String name, String address, String phone, String cuisineType, String status) {
            this.restaurantId = restaurantId;
            this.ownerId = ownerId;
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.cuisineType = cuisineType;
            this.status = status;
        }

        public int getRestaurantId() { return restaurantId; }
        public int getOwnerId() { return ownerId; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getCuisineType() { return cuisineType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return String.format("[ID: %d] %s | Cuisine: %s | Address: %s | Phone: %s | Status: %s",
                    restaurantId, name, cuisineType, address, phone, status);
        }
    }

    public static class MenuItem {
        private int menuItemId;
        private int restaurantId;
        private String itemName;
        private String description;
        private BigDecimal price;
        private String category;
        private boolean isAvailable;

        public MenuItem(int menuItemId, int restaurantId, String itemName, String description, BigDecimal price, String category, boolean isAvailable) {
            this.menuItemId = menuItemId;
            this.restaurantId = restaurantId;
            this.itemName = itemName;
            this.description = description;
            this.price = price;
            this.category = category;
            this.isAvailable = isAvailable;
        }

        public int getMenuItemId() { return menuItemId; }
        public int getRestaurantId() { return restaurantId; }
        public String getItemName() { return itemName; }
        public String getDescription() { return description; }
        public BigDecimal getPrice() { return price; }
        public String getCategory() { return category; }
        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }

        @Override
        public String toString() {
            return String.format("[Item #%d] %-22s | ₹%-7.2f | %-12s | %s",
                    menuItemId, itemName, price, category, (isAvailable ? "Available" : "Sold Out"));
        }
    }

    public static class CartItem {
        private int menuItemId;
        private String itemName;
        private BigDecimal unitPrice;
        private int quantity;

        public CartItem(int menuItemId, String itemName, BigDecimal unitPrice, int quantity) {
            this.menuItemId = menuItemId;
            this.itemName = itemName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public int getMenuItemId() { return menuItemId; }
        public String getItemName() { return itemName; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

        @Override
        public String toString() {
            return String.format("%-22s x %2d @ ₹%.2f = ₹%.2f", itemName, quantity, unitPrice, getSubtotal());
        }
    }

    public static class OrderItem {
        private int menuItemId;
        private String itemName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;

        public OrderItem(int menuItemId, String itemName, BigDecimal unitPrice, int quantity) {
            this.menuItemId = menuItemId;
            this.itemName = itemName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        public int getMenuItemId() { return menuItemId; }
        public String getItemName() { return itemName; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public int getQuantity() { return quantity; }
        public BigDecimal getSubtotal() { return subtotal; }
    }

    public static class Order {
        private int orderId;
        private int customerId;
        private int restaurantId;
        private BigDecimal totalAmount;
        private String status; // PENDING, CONFIRMED, PREPARING, READY, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
        private String deliveryAddress;
        private List<OrderItem> items;
        private LocalDateTime orderTime;

        public Order(int orderId, int customerId, int restaurantId, BigDecimal totalAmount, String deliveryAddress, List<OrderItem> items) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.restaurantId = restaurantId;
            this.totalAmount = totalAmount;
            this.deliveryAddress = deliveryAddress;
            this.items = items;
            this.status = "PENDING";
            this.orderTime = LocalDateTime.now();
        }

        public int getOrderId() { return orderId; }
        public int getCustomerId() { return customerId; }
        public int getRestaurantId() { return restaurantId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public List<OrderItem> getItems() { return items; }
        public LocalDateTime getOrderTime() { return orderTime; }

        @Override
        public String toString() {
            return String.format("[Order #%d] Total: ₹%.2f | Status: %-12s | Items: %d | Address: %s",
                    orderId, totalAmount, status, items.size(), deliveryAddress);
        }
    }

    public static class Payment {
        private int paymentId;
        private int orderId;
        private BigDecimal amount;
        private String paymentMethod; // ONLINE, COD
        private String paymentStatus; // SUCCESS, FAILED
        private String transactionRef;

        public Payment(int paymentId, int orderId, BigDecimal amount, String paymentMethod, String paymentStatus, String transactionRef) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentStatus = paymentStatus;
            this.transactionRef = transactionRef;
        }

        public int getPaymentId() { return paymentId; }
        public int getOrderId() { return orderId; }
        public BigDecimal getAmount() { return amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getTransactionRef() { return transactionRef; }

        @Override
        public String toString() {
            return String.format("[Payment #%d] Order: #%d | ₹%.2f | Method: %s | Status: %s | Ref: %s",
                    paymentId, orderId, amount, paymentMethod, paymentStatus, transactionRef);
        }
    }

    public static class Delivery {
        private int deliveryId;
        private int orderId;
        private int deliveryPartnerId;
        private String deliveryStatus; // ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED
        private LocalDateTime updatedAt;

        public Delivery(int deliveryId, int orderId, int deliveryPartnerId, String deliveryStatus) {
            this.deliveryId = deliveryId;
            this.orderId = orderId;
            this.deliveryPartnerId = deliveryPartnerId;
            this.deliveryStatus = deliveryStatus;
            this.updatedAt = LocalDateTime.now();
        }

        public int getDeliveryId() { return deliveryId; }
        public int getOrderId() { return orderId; }
        public int getDeliveryPartnerId() { return deliveryPartnerId; }
        public String getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(String deliveryStatus) {
            this.deliveryStatus = deliveryStatus;
            this.updatedAt = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("[Delivery #%d] Order: #%d | Partner ID: %d | Status: %s",
                    deliveryId, orderId, deliveryPartnerId, deliveryStatus);
        }
    }

    public static class Review {
        private int reviewId;
        private int customerId;
        private int restaurantId;
        private int orderId;
        private int rating; // 1 to 5
        private String comment;

        public Review(int reviewId, int customerId, int restaurantId, int orderId, int rating, String comment) {
            this.reviewId = reviewId;
            this.customerId = customerId;
            this.restaurantId = restaurantId;
            this.orderId = orderId;
            this.rating = rating;
            this.comment = comment;
        }

        public int getReviewId() { return reviewId; }
        public int getRestaurantId() { return restaurantId; }
        public int getOrderId() { return orderId; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }

        @Override
        public String toString() {
            return String.format("[Review #%d] Order #%d | Rating: %d/5 ★ | \"%s\"", reviewId, orderId, rating, comment);
        }
    }

    // ========================================================================
    // 2. IN-MEMORY DATABASE & SERVICE LAYER
    // ========================================================================

    public static class FoodDeliveryService {
        private final Map<Integer, User> users = new LinkedHashMap<>();
        private final Map<Integer, Restaurant> restaurants = new LinkedHashMap<>();
        private final Map<Integer, MenuItem> menuItems = new LinkedHashMap<>();
        private final Map<Integer, Map<Integer, CartItem>> userCarts = new HashMap<>();
        private final Map<Integer, Order> orders = new LinkedHashMap<>();
        private final List<Payment> payments = new ArrayList<>();
        private final Map<Integer, Delivery> deliveries = new LinkedHashMap<>();
        private final List<Review> reviews = new ArrayList<>();

        private int nextUserId = 1;
        private int nextRestaurantId = 1;
        private int nextMenuItemId = 1;
        private int nextOrderId = 1001;
        private int nextPaymentId = 5001;
        private int nextDeliveryId = 8001;
        private int nextReviewId = 1;

        public FoodDeliveryService() {
            seedSampleData();
        }

        private void seedSampleData() {
            // Seed Users
            registerUser("Alice Customer", "customer@example.com", "pass123", "9876543210", "CUSTOMER");
            registerUser("Gourmet Kitchen Owner", "restaurant@example.com", "pass123", "9876543211", "RESTAURANT");
            registerUser("Bob Rider", "delivery@example.com", "pass123", "9876543212", "DELIVERY_PARTNER");
            registerUser("System Admin", "admin@example.com", "admin123", "9876543213", "ADMIN");

            // Seed Restaurant
            createRestaurant(2, "Gourmet Bistro", "101 Grand Avenue", "555-0100", "Continental", "ACTIVE");
            createRestaurant(2, "Spice Route Indian", "45 Park Street", "555-0200", "Indian & Mughlai", "ACTIVE");

            // Seed Menu Items for Restaurant 1
            addMenuItem(1, "Classic Caesar Salad", "Crisp romaine, parmesan, garlic croutons", new BigDecimal("180.00"), "Starters");
            addMenuItem(1, "Herb Roasted Chicken", "Slow-roasted chicken breast with veggies", new BigDecimal("350.00"), "Main Course");
            addMenuItem(1, "Creamy Alfredo Pasta", "Fettuccine pasta in rich parmesan cheese sauce", new BigDecimal("290.00"), "Main Course");
            addMenuItem(1, "Chocolate Lava Cake", "Warm molten chocolate center with vanilla scoop", new BigDecimal("150.00"), "Desserts");
            addMenuItem(1, "Fresh Mango Smoothie", "Chilled organic Alphonso mango puree", new BigDecimal("120.00"), "Beverages");

            // Seed Menu Items for Restaurant 2
            addMenuItem(2, "Paneer Butter Masala", "Cottage cheese cubes in spiced tomato gravy", new BigDecimal("280.00"), "Curries");
            addMenuItem(2, "Butter Garlic Naan", "Traditional clay oven baked flatbread", new BigDecimal("60.00"), "Breads");
            addMenuItem(2, "Dum Biryani Special", "Fragrant Basmati rice cooked with spices", new BigDecimal("320.00"), "Rice");
        }

        // --- User Service ---
        public User registerUser(String name, String email, String password, String phone, String role) {
            for (User u : users.values()) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    System.out.println("❌ Registration Failed: Email already registered.");
                    return null;
                }
            }
            User user = new User(nextUserId++, name, email, password, phone, role.toUpperCase());
            users.put(user.getUserId(), user);
            return user;
        }

        public User login(String email, String password) {
            for (User u : users.values()) {
                if (u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password)) {
                    return u;
                }
            }
            return null;
        }

        // --- Restaurant Service ---
        public Restaurant createRestaurant(int ownerId, String name, String address, String phone, String cuisine, String status) {
            User owner = users.get(ownerId);
            if (owner == null || !"RESTAURANT".equals(owner.getRole())) {
                System.out.println("❌ Only users with RESTAURANT role can register a restaurant.");
                return null;
            }
            if (name == null || name.trim().isEmpty()) {
                System.out.println("❌ Restaurant name cannot be empty.");
                return null;
            }
            Restaurant r = new Restaurant(nextRestaurantId++, ownerId, name, address, phone, cuisine, status);
            restaurants.put(r.getRestaurantId(), r);
            return r;
        }

        public List<Restaurant> getAllRestaurants() {
            return new ArrayList<>(restaurants.values());
        }

        public Restaurant getRestaurant(int restaurantId) {
            return restaurants.get(restaurantId);
        }

        // --- Menu Item Service ---
        public MenuItem addMenuItem(int restaurantId, String name, String desc, BigDecimal price, String category) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("❌ Price must be greater than zero.");
                return null;
            }
            MenuItem item = new MenuItem(nextMenuItemId++, restaurantId, name, desc, price, category, true);
            menuItems.put(item.getMenuItemId(), item);
            return item;
        }

        public List<MenuItem> getMenuByRestaurant(int restaurantId) {
            List<MenuItem> list = new ArrayList<>();
            for (MenuItem item : menuItems.values()) {
                if (item.getRestaurantId() == restaurantId) {
                    list.add(item);
                }
            }
            return list;
        }

        public MenuItem getMenuItem(int menuItemId) {
            return menuItems.get(menuItemId);
        }

        // --- Cart Service ---
        public boolean addToCart(int userId, int menuItemId, int quantity) {
            if (quantity <= 0) {
                System.out.println("❌ Quantity must be at least 1.");
                return false;
            }
            MenuItem item = menuItems.get(menuItemId);
            if (item == null || !item.isAvailable()) {
                System.out.println("❌ Menu item is not available.");
                return false;
            }

            userCarts.putIfAbsent(userId, new LinkedHashMap<>());
            Map<Integer, CartItem> cart = userCarts.get(userId);

            if (cart.containsKey(menuItemId)) {
                CartItem existing = cart.get(menuItemId);
                existing.setQuantity(existing.getQuantity() + quantity);
            } else {
                cart.put(menuItemId, new CartItem(menuItemId, item.getItemName(), item.getPrice(), quantity));
            }
            return true;
        }

        public List<CartItem> viewCart(int userId) {
            Map<Integer, CartItem> cart = userCarts.get(userId);
            if (cart == null || cart.isEmpty()) return Collections.emptyList();
            return new ArrayList<>(cart.values());
        }

        public BigDecimal getCartTotal(int userId) {
            BigDecimal total = BigDecimal.ZERO;
            for (CartItem item : viewCart(userId)) {
                total = total.add(item.getSubtotal());
            }
            return total;
        }

        public void clearCart(int userId) {
            if (userCarts.containsKey(userId)) {
                userCarts.get(userId).clear();
            }
        }

        // --- Order Service ---
        public Order checkout(int userId, int restaurantId, String deliveryAddress) {
            List<CartItem> cartItems = viewCart(userId);
            if (cartItems.isEmpty()) {
                System.out.println("❌ Cannot checkout an empty cart.");
                return null;
            }
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                System.out.println("❌ Delivery address is mandatory.");
                return null;
            }

            BigDecimal total = getCartTotal(userId);
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem ci : cartItems) {
                orderItems.add(new OrderItem(ci.getMenuItemId(), ci.getItemName(), ci.getUnitPrice(), ci.getQuantity()));
            }

            Order order = new Order(nextOrderId++, userId, restaurantId, total, deliveryAddress, orderItems);
            orders.put(order.getOrderId(), order);
            clearCart(userId);
            return order;
        }

        public Order getOrder(int orderId) {
            return orders.get(orderId);
        }

        public List<Order> getOrdersByUser(int userId) {
            List<Order> list = new ArrayList<>();
            for (Order o : orders.values()) {
                if (o.getCustomerId() == userId) list.add(o);
            }
            return list;
        }

        public List<Order> getAllOrders() {
            return new ArrayList<>(orders.values());
        }

        // --- Payment Service ---
        public Payment processPayment(int orderId, String method, boolean simulateSuccess) {
            Order order = orders.get(orderId);
            if (order == null) {
                System.out.println("❌ Order not found.");
                return null;
            }
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("❌ Payment already processed or order not in PENDING state.");
                return null;
            }

            String ref = "TXN-" + System.currentTimeMillis();
            String status = simulateSuccess ? "SUCCESS" : "FAILED";
            Payment payment = new Payment(nextPaymentId++, orderId, order.getTotalAmount(), method, status, ref);
            payments.add(payment);

            if (simulateSuccess) {
                order.setStatus("CONFIRMED");
            }
            return payment;
        }

        // --- Delivery Lifecycle Service ---
        public boolean updateOrderStatusByRestaurant(int orderId, String newStatus) {
            Order order = orders.get(orderId);
            if (order == null) return false;
            order.setStatus(newStatus);
            return true;
        }

        public Delivery assignDelivery(int deliveryPartnerId, int orderId) {
            Order order = orders.get(orderId);
            if (order == null) {
                System.out.println("❌ Order not found.");
                return null;
            }
            if (!"READY".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
                System.out.println("❌ Order is not ready for delivery assignment yet.");
                return null;
            }

            Delivery delivery = new Delivery(nextDeliveryId++, orderId, deliveryPartnerId, "ASSIGNED");
            deliveries.put(orderId, delivery);
            order.setStatus("OUT_FOR_DELIVERY");
            return delivery;
        }

        public boolean advanceDeliveryStatus(int orderId, String newStatus) {
            Delivery delivery = deliveries.get(orderId);
            Order order = orders.get(orderId);
            if (delivery == null || order == null) return false;

            delivery.setDeliveryStatus(newStatus);
            if ("DELIVERED".equals(newStatus)) {
                order.setStatus("DELIVERED");
            }
            return true;
        }

        // --- Review Service ---
        public Review addReview(int customerId, int restaurantId, int orderId, int rating, String comment) {
            Order order = orders.get(orderId);
            if (order == null || order.getCustomerId() != customerId) {
                System.out.println("❌ Review must be submitted by the customer who placed the order.");
                return null;
            }
            if (!"DELIVERED".equals(order.getStatus())) {
                System.out.println("❌ Can only review completed (DELIVERED) orders.");
                return null;
            }
            if (rating < 1 || rating > 5) {
                System.out.println("❌ Rating must be between 1 and 5 stars.");
                return null;
            }
            for (Review r : reviews) {
                if (r.getOrderId() == orderId) {
                    System.out.println("❌ Order has already been reviewed.");
                    return null;
                }
            }

            Review review = new Review(nextReviewId++, customerId, restaurantId, orderId, rating, comment);
            reviews.add(review);
            return review;
        }

        public double getAverageRating(int restaurantId) {
            int count = 0;
            int sum = 0;
            for (Review r : reviews) {
                if (r.getRestaurantId() == restaurantId) {
                    sum += r.getRating();
                    count++;
                }
            }
            return count == 0 ? 0.0 : (double) sum / count;
        }

        public List<Review> getReviewsByRestaurant(int restaurantId) {
            List<Review> list = new ArrayList<>();
            for (Review r : reviews) {
                if (r.getRestaurantId() == restaurantId) list.add(r);
            }
            return list;
        }

        // --- Admin Statistics ---
        public void displayAdminAnalytics() {
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║            SYSTEM ADMINISTRATOR ANALYTICS DASHBOARD          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.printf("║ Total Registered Users       : %-29d ║%n", users.size());
            System.out.printf("║ Active Restaurants           : %-29d ║%n", restaurants.size());
            System.out.printf("║ Catalog Menu Items           : %-29d ║%n", menuItems.size());
            System.out.printf("║ Total Orders Placed          : %-29d ║%n", orders.size());

            BigDecimal totalRevenue = BigDecimal.ZERO;
            int deliveredCount = 0;
            for (Order o : orders.values()) {
                if ("DELIVERED".equals(o.getStatus()) || "CONFIRMED".equals(o.getStatus())) {
                    totalRevenue = totalRevenue.add(o.getTotalAmount());
                }
                if ("DELIVERED".equals(o.getStatus())) deliveredCount++;
            }

            System.out.printf("║ Total Orders Completed       : %-29d ║%n", deliveredCount);
            System.out.printf("║ Gross System Revenue         : ₹%-28.2f ║%n", totalRevenue);
            System.out.printf("║ Total Customer Reviews       : %-29d ║%n", reviews.size());
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
        }
    }

    // ========================================================================
    // 3. AUTOMATED VERIFICATION SUITE & INTERACTIVE RUNNER
    // ========================================================================

    public static void runAutomatedSystemVerification(FoodDeliveryService service) {
        System.out.println("\n======================================================================");
        System.out.println("        ONLINE FOOD DELIVERY APPLICATION - SYSTEM WORKFLOW DEMO       ");
        System.out.println("======================================================================");

        // Step 1: User Registration & Authentication
        System.out.println("\n[1] USER REGISTRATION & AUTHENTICATION");
        User customer = service.login("customer@example.com", "pass123");
        User restaurantOwner = service.login("restaurant@example.com", "pass123");
        User rider = service.login("delivery@example.com", "pass123");
        User admin = service.login("admin@example.com", "admin123");

        System.out.println(" ✔ Customer logged in        : " + customer);
        System.out.println(" ✔ Restaurant Owner logged in : " + restaurantOwner);
        System.out.println(" ✔ Delivery Rider logged in  : " + rider);
        System.out.println(" ✔ Administrator logged in   : " + admin);

        // Step 2: Restaurant & Menu Browsing
        System.out.println("\n[2] RESTAURANT & MENU CATALOG");
        List<Restaurant> restaurants = service.getAllRestaurants();
        for (Restaurant r : restaurants) {
            System.out.println(" 🏬 " + r);
            List<MenuItem> menu = service.getMenuByRestaurant(r.getRestaurantId());
            for (MenuItem item : menu) {
                System.out.println("     • " + item);
            }
        }

        // Step 3: Shopping Cart Operations
        System.out.println("\n[3] SHOPPING CART OPERATIONS FOR ALICE CUSTOMER");
        service.addToCart(customer.getUserId(), 1, 2); // 2x Caesar Salad
        service.addToCart(customer.getUserId(), 2, 1); // 1x Herb Roasted Chicken
        service.addToCart(customer.getUserId(), 4, 1); // 1x Chocolate Lava Cake

        System.out.println(" Cart Contents:");
        for (CartItem ci : service.viewCart(customer.getUserId())) {
            System.out.println("   -> " + ci);
        }
        System.out.printf("   Cart Subtotal: ₹%.2f%n", service.getCartTotal(customer.getUserId()));

        // Step 4: Checkout & Order Placement
        System.out.println("\n[4] ORDER CHECKOUT");
        Order order = service.checkout(customer.getUserId(), 1, "Flat 402, Green Valley Apartments, Mumbai");
        System.out.println(" ✔ Order Successfully Created: " + order);

        // Step 5: Payment Processing
        System.out.println("\n[5] PAYMENT PROCESSING (ONLINE UPI/CARD)");
        Payment payment = service.processPayment(order.getOrderId(), "ONLINE", true);
        System.out.println(" ✔ Payment Executed: " + payment);
        System.out.println(" ✔ Order Status Updated to: " + order.getStatus());

        // Step 6: Kitchen Preparation Workflow
        System.out.println("\n[6] KITCHEN STATUS WORKFLOW");
        service.updateOrderStatusByRestaurant(order.getOrderId(), "PREPARING");
        System.out.println(" 🍳 Chef is preparing the order... Status: " + order.getStatus());
        service.updateOrderStatusByRestaurant(order.getOrderId(), "READY");
        System.out.println(" 📦 Order packed and ready for pickup! Status: " + order.getStatus());

        // Step 7: Delivery Assignment and Real-Time Tracking
        System.out.println("\n[7] DELIVERY PARTNER ASSIGNMENT & STATUS TRANSITIONS");
        Delivery delivery = service.assignDelivery(rider.getUserId(), order.getOrderId());
        System.out.println(" 🛵 Assigned Delivery Partner: " + delivery);

        service.advanceDeliveryStatus(order.getOrderId(), "PICKED_UP");
        System.out.println(" -> Rider has picked up order from restaurant. Current Delivery Status: PICKED_UP");

        service.advanceDeliveryStatus(order.getOrderId(), "OUT_FOR_DELIVERY");
        System.out.println(" -> Rider is heading to delivery location. Current Delivery Status: OUT_FOR_DELIVERY");

        service.advanceDeliveryStatus(order.getOrderId(), "DELIVERED");
        System.out.println(" ✔ Order successfully handed over to customer! Order Final Status: " + order.getStatus());

        // Step 8: Customer Review & Rating
        System.out.println("\n[8] CUSTOMER FEEDBACK & RESTAURANT RATING");
        Review review = service.addReview(customer.getUserId(), 1, order.getOrderId(), 5, "Exquisite food quality and prompt delivery!");
        System.out.println(" ✔ Submitted Review: " + review);
        System.out.printf(" ★ Restaurant Average Rating: %.1f / 5.0%n", service.getAverageRating(1));

        // Step 9: System Admin Analytics
        System.out.println("\n[9] ADMINISTRATOR REVENUE & ANALYTICS");
        service.displayAdminAnalytics();

        System.out.println("\n======================================================================");
        System.out.println("    ✔ ALL MODULE TESTS & OPERATIONS COMPLETED SUCCESSFULLY!           ");
        System.out.println("======================================================================\n");
    }

    // ========================================================================
    // 4. MAIN ENTRY POINT
    // ========================================================================

    public static void main(String[] args) {
        System.out.println("**********************************************************************");
        System.out.println("*              ONLINE FOOD DELIVERY APPLICATION - CORE JAVA          *");
        System.out.println("*              Author: Aryan Singh | Upskill Campus Internship       *");
        System.out.println("**********************************************************************");

        FoodDeliveryService service = new FoodDeliveryService();

        // If args contains "--interactive" or no arguments, run the automated verification
        // followed by interactive mode if console is available.
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            runInteractiveMenu(service);
        } else {
            // Default run executes full comprehensive verification demo
            runAutomatedSystemVerification(service);
            System.out.println("To enter interactive terminal mode, run: java FoodDeliveryApplication --cli");
        }
    }

    private static void runInteractiveMenu(FoodDeliveryService service) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n========== FOOD DELIVERY SYSTEM MENU ==========");
            System.out.println("1. View Restaurants");
            System.out.println("2. View Restaurant Menu");
            System.out.println("3. Run Automated System Simulation Demo");
            System.out.println("4. View Administrator Analytics");
            System.out.println("5. Exit");
            System.out.print("Enter choice (1-5): ");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\n--- Registered Restaurants ---");
                    for (Restaurant r : service.getAllRestaurants()) {
                        System.out.println(r);
                    }
                    break;
                case "2":
                    System.out.print("Enter Restaurant ID: ");
                    try {
                        int rId = Integer.parseInt(scanner.nextLine().trim());
                        List<MenuItem> items = service.getMenuByRestaurant(rId);
                        if (items.isEmpty()) {
                            System.out.println("No items found for Restaurant ID " + rId);
                        } else {
                            System.out.println("\n--- Menu for Restaurant " + rId + " ---");
                            for (MenuItem m : items) System.out.println(m);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format.");
                    }
                    break;
                case "3":
                    runAutomatedSystemVerification(service);
                    break;
                case "4":
                    service.displayAdminAnalytics();
                    break;
                case "5":
                    System.out.println("Exiting Food Delivery Application. Thank you!");
                    return;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 5.");
            }
        }
    }
}
