package dao;

import config.DatabaseConnection;
import model.Restaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {

    public boolean createRestaurant(Restaurant restaurant) {
        String sql = "INSERT INTO restaurants " +
                "(owner_id, name, address, phone, cuisine_type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, restaurant.getOwnerId());
            statement.setString(2, restaurant.getName());
            statement.setString(3, restaurant.getAddress());
            statement.setString(4, restaurant.getPhone());
            statement.setString(5, restaurant.getCuisineType());
            statement.setString(6, restaurant.getStatus());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Restaurant getRestaurantById(int restaurantId) {
        String sql = "SELECT * FROM restaurants WHERE restaurant_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, restaurantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToRestaurant(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Restaurant> getRestaurantsByOwnerId(int ownerId) {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants WHERE owner_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, ownerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(mapResultSetToRestaurant(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Restaurant> getAllRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurants";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(mapResultSetToRestaurant(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Restaurant mapResultSetToRestaurant(ResultSet resultSet) throws SQLException {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(resultSet.getInt("restaurant_id"));
        restaurant.setOwnerId(resultSet.getInt("owner_id"));
        restaurant.setName(resultSet.getString("name"));
        restaurant.setAddress(resultSet.getString("address"));
        restaurant.setPhone(resultSet.getString("phone"));
        restaurant.setCuisineType(resultSet.getString("cuisine_type"));
        restaurant.setStatus(resultSet.getString("status"));
        restaurant.setCreatedAt(resultSet.getTimestamp("created_at"));
        return restaurant;
    }

    public boolean updateRestaurant(Restaurant restaurant) {
        String sql = "UPDATE restaurants SET " +
                "name = ?, address = ?, phone = ?, cuisine_type = ?, status = ? " +
                "WHERE restaurant_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, restaurant.getName());
            statement.setString(2, restaurant.getAddress());
            statement.setString(3, restaurant.getPhone());
            statement.setString(4, restaurant.getCuisineType());
            statement.setString(5, restaurant.getStatus());
            statement.setInt(6, restaurant.getRestaurantId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
