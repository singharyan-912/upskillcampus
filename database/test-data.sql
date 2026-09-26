-- ==========================================================
-- TEST DATA ONLY — CONTROLLED SEED SCRIPT FOR FOOD DELIVERY APP
-- Tech Stack: MySQL, Java Servlets, JDBC, Frontend Fetch API
-- Safe & Idempotent: Does NOT drop, truncate, or delete user data.
-- ==========================================================

USE `food_delivery_db`;

-- ----------------------------------------------------------
-- 1. CONTROLLED TEST USERS (All roles, password >= 6 chars)
-- ----------------------------------------------------------
INSERT INTO `users` (`name`, `email`, `password`, `phone`, `role`)
VALUES
  ('Test Customer', 'customer.test@foodapp.local', 'Test@123', '9876543210', 'CUSTOMER'),
  ('Test Restaurant Owner', 'restaurant.test@foodapp.local', 'Test@123', '9876543211', 'RESTAURANT'),
  ('Test Delivery Partner', 'delivery.test@foodapp.local', 'Test@123', '9876543212', 'DELIVERY_PARTNER'),
  ('Test Admin', 'admin.test@foodapp.local', 'Test@123', '9876543213', 'ADMIN')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `password` = VALUES(`password`),
  `phone` = VALUES(`phone`),
  `role` = VALUES(`role`);

-- Resolve User IDs into session variables
SET @cust_id := (SELECT `user_id` FROM `users` WHERE `email` = 'customer.test@foodapp.local' LIMIT 1);
SET @owner_id := (SELECT `user_id` FROM `users` WHERE `email` = 'restaurant.test@foodapp.local' LIMIT 1);
SET @driver_id := (SELECT `user_id` FROM `users` WHERE `email` = 'delivery.test@foodapp.local' LIMIT 1);
SET @admin_id := (SELECT `user_id` FROM `users` WHERE `email` = 'admin.test@foodapp.local' LIMIT 1);

-- ----------------------------------------------------------
-- 2. CONTROLLED TEST RESTAURANTS (Owned by Test Restaurant Owner)
-- ----------------------------------------------------------
INSERT INTO `restaurants` (`owner_id`, `name`, `address`, `phone`, `cuisine_type`, `status`)
SELECT @owner_id, 'Test Biryani House', '12 MG Road, Bangalore', '080-41234561', 'Biryani', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `restaurants` WHERE `name` = 'Test Biryani House' AND `owner_id` = @owner_id);

INSERT INTO `restaurants` (`owner_id`, `name`, `address`, `phone`, `cuisine_type`, `status`)
SELECT @owner_id, 'Test Pizza Corner', '45 Indiranagar, Bangalore', '080-41234562', 'Pizza', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `restaurants` WHERE `name` = 'Test Pizza Corner' AND `owner_id` = @owner_id);

INSERT INTO `restaurants` (`owner_id`, `name`, `address`, `phone`, `cuisine_type`, `status`)
SELECT @owner_id, 'Test Burger Kitchen', '78 Koramangala, Bangalore', '080-41234563', 'Fast Food', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `restaurants` WHERE `name` = 'Test Burger Kitchen' AND `owner_id` = @owner_id);

-- Resolve Restaurant IDs into session variables
SET @rest_biryani_id := (SELECT `restaurant_id` FROM `restaurants` WHERE `name` = 'Test Biryani House' AND `owner_id` = @owner_id LIMIT 1);
SET @rest_pizza_id := (SELECT `restaurant_id` FROM `restaurants` WHERE `name` = 'Test Pizza Corner' AND `owner_id` = @owner_id LIMIT 1);
SET @rest_burger_id := (SELECT `restaurant_id` FROM `restaurants` WHERE `name` = 'Test Burger Kitchen' AND `owner_id` = @owner_id LIMIT 1);

-- ----------------------------------------------------------
-- 3. CONTROLLED MENU ITEMS (4 per restaurant, total 12 items)
-- ----------------------------------------------------------
-- Test Biryani House
INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_biryani_id, 'Chicken Biryani', 'Aromatic basmati rice cooked with spiced tender chicken', 260.00, 'Biryani', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Chicken Biryani');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_biryani_id, 'Mutton Biryani', 'Slow-cooked fragrant rice with succulent lamb chunks', 340.00, 'Biryani', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Mutton Biryani');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_biryani_id, 'Paneer Biryani', 'Layered spiced rice cooked with marinated cottage cheese', 220.00, 'Biryani', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Paneer Biryani');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_biryani_id, 'Cucumber Raita', 'Cool spiced yogurt accompaniment with fresh cucumber', 40.00, 'Starter', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Cucumber Raita');

-- Test Pizza Corner
INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_pizza_id, 'Margherita Pizza', 'Classic hand-tossed crust with san marzano tomato and mozzarella', 199.00, 'Pizza', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_pizza_id AND `item_name` = 'Margherita Pizza');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_pizza_id, 'Farmhouse Veg Pizza', 'Loaded with bell peppers, crisp onions, mushrooms, and olives', 299.00, 'Pizza', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_pizza_id AND `item_name` = 'Farmhouse Veg Pizza');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_pizza_id, 'Paneer Tikka Pizza', 'Tandoori spiced paneer cubes with mozzarella and chili flakes', 329.00, 'Pizza', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_pizza_id AND `item_name` = 'Paneer Tikka Pizza');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_pizza_id, 'Garlic Breadsticks', 'Oven baked bread infused with roasted garlic butter and herbs', 99.00, 'Starter', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_pizza_id AND `item_name` = 'Garlic Breadsticks');

-- Test Burger Kitchen
INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_burger_id, 'Crispy Veg Burger', 'Golden crispy vegetable patty with creamy lettuce and mayo', 129.00, 'Burger', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_burger_id AND `item_name` = 'Crispy Veg Burger');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_burger_id, 'Classic Chicken Burger', 'Juicy grilled chicken fillet with caramelized onions and cheddar', 189.00, 'Burger', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_burger_id AND `item_name` = 'Classic Chicken Burger');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_burger_id, 'Peri Peri French Fries', 'Deep-fried golden potato wedges seasoned with fiery peri-peri spices', 89.00, 'Starter', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_burger_id AND `item_name` = 'Peri Peri French Fries');

INSERT INTO `menu_items` (`restaurant_id`, `item_name`, `description`, `price`, `category`, `availability`)
SELECT @rest_burger_id, 'Cold Coffee', 'Thick blended iced coffee topped with chocolate syrup', 79.00, 'Beverage', 0
WHERE NOT EXISTS (SELECT 1 FROM `menu_items` WHERE `restaurant_id` = @rest_burger_id AND `item_name` = 'Cold Coffee');

-- Resolve Menu Item IDs
SET @item_chk_biryani := (SELECT `menu_item_id` FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Chicken Biryani' LIMIT 1);
SET @item_mut_biryani := (SELECT `menu_item_id` FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Mutton Biryani' LIMIT 1);
SET @item_pan_biryani := (SELECT `menu_item_id` FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Paneer Biryani' LIMIT 1);
SET @item_raita := (SELECT `menu_item_id` FROM `menu_items` WHERE `restaurant_id` = @rest_biryani_id AND `item_name` = 'Cucumber Raita' LIMIT 1);

-- ----------------------------------------------------------
-- 4. CONTROLLED ACTIVE CART (For Test Customer at Test Biryani House)
-- ----------------------------------------------------------
INSERT INTO `carts` (`user_id`, `restaurant_id`, `status`)
SELECT @cust_id, @rest_biryani_id, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `carts` WHERE `user_id` = @cust_id AND `status` = 'ACTIVE');

SET @cart_active_id := (SELECT `cart_id` FROM `carts` WHERE `user_id` = @cust_id AND `status` = 'ACTIVE' LIMIT 1);

INSERT INTO `cart_items` (`cart_id`, `menu_item_id`, `quantity`, `unit_price`)
VALUES
  (@cart_active_id, @item_chk_biryani, 2, 260.00),
  (@cart_active_id, @item_raita, 1, 40.00)
ON DUPLICATE KEY UPDATE
  `quantity` = VALUES(`quantity`),
  `unit_price` = VALUES(`unit_price`);

-- ----------------------------------------------------------
-- 5. CONTROLLED TEST ORDERS (All distinct lifecycle states)
-- ----------------------------------------------------------

-- ORDER 1: PLACED (Payment PENDING via COD)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 220.00, 'PLACED', 'Flat 101, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 101, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'PLACED');

SET @order1_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'PLACED' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order1_id, @item_pan_biryani, 'Paneer Biryani', 1, 220.00, 220.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order1_id);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order1_id, 220.00, 'COD', 'PENDING', NULL
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order1_id);


-- ORDER 2: CONFIRMED (Online payment SUCCESS)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 260.00, 'CONFIRMED', 'Flat 202, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 202, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'CONFIRMED');

SET @order2_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'CONFIRMED' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order2_id, @item_chk_biryani, 'Chicken Biryani', 1, 260.00, 260.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order2_id);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order2_id, 260.00, 'ONLINE', 'SUCCESS', 'TXN-TESTCONF01'
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order2_id);


-- ORDER 3: PREPARING (Cooking in kitchen)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 340.00, 'PREPARING', 'Flat 303, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 303, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'PREPARING');

SET @order3_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'PREPARING' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order3_id, @item_mut_biryani, 'Mutton Biryani', 1, 340.00, 340.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order3_id);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order3_id, 340.00, 'ONLINE', 'SUCCESS', 'TXN-TESTPREP02'
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order3_id);


-- ORDER 4: READY (Awaiting Delivery Partner self-assignment in Available Orders)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 300.00, 'READY', 'Flat 404, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 404, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'READY');

SET @order4_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'READY' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order4_id, @item_chk_biryani, 'Chicken Biryani', 1, 260.00, 260.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order4_id AND `menu_item_id` = @item_chk_biryani);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order4_id, @item_raita, 'Cucumber Raita', 1, 40.00, 40.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order4_id AND `menu_item_id` = @item_raita);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order4_id, 300.00, 'ONLINE', 'SUCCESS', 'TXN-TESTREDY03'
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order4_id);


-- ORDER 5: OUT_FOR_DELIVERY (Active delivery assigned to Test Delivery Partner)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 260.00, 'OUT_FOR_DELIVERY', 'Flat 505, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 505, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'OUT_FOR_DELIVERY');

SET @order5_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'OUT_FOR_DELIVERY' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order5_id, @item_chk_biryani, 'Chicken Biryani', 1, 260.00, 260.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order5_id);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order5_id, 260.00, 'ONLINE', 'SUCCESS', 'TXN-TESTOFD04'
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order5_id);

INSERT INTO `deliveries` (`order_id`, `delivery_partner_id`, `delivery_status`, `assigned_time`, `pickup_time`, `delivered_time`)
SELECT @order5_id, @driver_id, 'OUT_FOR_DELIVERY', NOW() - INTERVAL 30 MINUTE, NOW() - INTERVAL 15 MINUTE, NULL
WHERE NOT EXISTS (SELECT 1 FROM `deliveries` WHERE `order_id` = @order5_id);


-- ORDER 6: DELIVERED (Completed lifecycle: Delivered + Reviewed)
INSERT INTO `orders` (`user_id`, `restaurant_id`, `total_amount`, `status`, `delivery_address`)
SELECT @cust_id, @rest_biryani_id, 380.00, 'DELIVERED', 'Flat 606, Palm Heights, 12 MG Road, Bangalore'
WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE `user_id` = @cust_id AND `delivery_address` = 'Flat 606, Palm Heights, 12 MG Road, Bangalore' AND `status` = 'DELIVERED');

SET @order6_id := (SELECT `order_id` FROM `orders` WHERE `user_id` = @cust_id AND `status` = 'DELIVERED' ORDER BY `order_id` DESC LIMIT 1);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order6_id, @item_mut_biryani, 'Mutton Biryani', 1, 340.00, 340.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order6_id AND `menu_item_id` = @item_mut_biryani);

INSERT INTO `order_items` (`order_id`, `menu_item_id`, `item_name`, `quantity`, `unit_price`, `subtotal`)
SELECT @order6_id, @item_raita, 'Cucumber Raita', 1, 40.00, 40.00
WHERE NOT EXISTS (SELECT 1 FROM `order_items` WHERE `order_id` = @order6_id AND `menu_item_id` = @item_raita);

INSERT INTO `payments` (`order_id`, `amount`, `payment_method`, `payment_status`, `transaction_ref`)
SELECT @order6_id, 380.00, 'ONLINE', 'SUCCESS', 'TXN-TESTDELV05'
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `order_id` = @order6_id);

INSERT INTO `deliveries` (`order_id`, `delivery_partner_id`, `delivery_status`, `assigned_time`, `pickup_time`, `delivered_time`)
SELECT @order6_id, @driver_id, 'DELIVERED', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 105 MINUTE, NOW() - INTERVAL 80 MINUTE
WHERE NOT EXISTS (SELECT 1 FROM `deliveries` WHERE `order_id` = @order6_id);

INSERT INTO `reviews` (`order_id`, `customer_id`, `restaurant_id`, `rating`, `review_comment`, `created_at`)
SELECT @order6_id, @cust_id, @rest_biryani_id, 5, 'Good food and fast delivery.', NOW() - INTERVAL 75 MINUTE
WHERE NOT EXISTS (SELECT 1 FROM `reviews` WHERE `order_id` = @order6_id);

-- End of test-data.sql
