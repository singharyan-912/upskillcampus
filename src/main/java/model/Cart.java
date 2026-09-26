package model;

import java.sql.Timestamp;

public class Cart {

    private int cartId;
    private int userId;
    private int restaurantId;
    private String status;
    private Timestamp createdAt;

    // Default Constructor
    public Cart() {
    }

    // Constructor for creating a new cart
    public Cart(int userId, int restaurantId) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.status = "ACTIVE";
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
