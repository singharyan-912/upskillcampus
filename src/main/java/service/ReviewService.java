package service;

import dao.OrderDAO;
import dao.ReviewDAO;
import model.Order;
import model.Review;

import java.util.List;

public class ReviewService {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public boolean submitReview(int customerId, int orderId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            System.out.println("Invalid rating. Rating must be between 1 and 5.");
            return false;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            System.out.println("Order not found.");
            return false;
        }

        if (order.getUserId() != customerId) {
            System.out.println("Access denied. You can only review your own orders.");
            return false;
        }

        if (!"DELIVERED".equals(order.getStatus())) {
            System.out.println("You can only review an order after it has been delivered.");
            return false;
        }

        if (reviewDAO.hasReviewed(orderId)) {
            System.out.println("You have already reviewed this order.");
            return false;
        }

        Review review = new Review(orderId, customerId, order.getRestaurantId(), rating, comment);
        boolean success = reviewDAO.createReview(review);
        if (success) {
            System.out.println("Review submitted successfully!");
        }
        return success;
    }

    public List<Review> getReviewsForRestaurant(int restaurantId) {
        return reviewDAO.getReviewsByRestaurantId(restaurantId);
    }

    public double getAverageRating(int restaurantId) {
        return reviewDAO.getAverageRatingForRestaurant(restaurantId);
    }
}
