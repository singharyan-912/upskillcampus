package dao;

import config.DatabaseConnection;
import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public boolean createReview(Review review) {
        String sql = "INSERT INTO reviews (order_id, customer_id, restaurant_id, rating, review_comment) VALUES (?, ?, ?, ?, ?)";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, review.getOrderId());
            statement.setInt(2, review.getCustomerId());
            statement.setInt(3, review.getRestaurantId());
            statement.setInt(4, review.getRating());
            statement.setString(5, review.getReviewComment());

            int rows = statement.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        review.setReviewId(keys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            // Log duplicated review error explicitly if helpful, else generic
            System.out.println("Error saving review. " + e.getMessage());
        }
        return false;
    }

    public boolean hasReviewed(int orderId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE order_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Review> getReviewsByRestaurantId(int restaurantId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM reviews WHERE restaurant_id = ? ORDER BY created_at DESC";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, restaurantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reviews.add(mapResultSetToReview(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    public double getAverageRatingForRestaurant(int restaurantId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE restaurant_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, restaurantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setOrderId(rs.getInt("order_id"));
        review.setCustomerId(rs.getInt("customer_id"));
        review.setRestaurantId(rs.getInt("restaurant_id"));
        review.setRating(rs.getInt("rating"));
        review.setReviewComment(rs.getString("review_comment"));
        review.setCreatedAt(rs.getTimestamp("created_at"));
        return review;
    }
}
