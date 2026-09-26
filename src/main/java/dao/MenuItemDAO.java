package dao;

import config.DatabaseConnection;
import model.MenuItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    public boolean addMenuItem(MenuItem item) {
        String sql = "INSERT INTO menu_items " +
                "(restaurant_id, item_name, description, price, category, availability) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, item.getRestaurantId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setBigDecimal(4, item.getPrice());
            statement.setString(5, item.getCategory());
            statement.setBoolean(6, item.isAvailable());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<MenuItem> getMenuItemsByRestaurantId(int restaurantId) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE restaurant_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, restaurantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(mapResultSetToMenuItem(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public MenuItem getMenuItemById(int menuItemId) {
        String sql = "SELECT * FROM menu_items WHERE menu_item_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, menuItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToMenuItem(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateMenuItem(MenuItem item) {
        String sql = "UPDATE menu_items SET " +
                "item_name = ?, description = ?, price = ?, category = ?, availability = ? " +
                "WHERE menu_item_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, item.getItemName());
            statement.setString(2, item.getDescription());
            statement.setBigDecimal(3, item.getPrice());
            statement.setString(4, item.getCategory());
            statement.setBoolean(5, item.isAvailable());
            statement.setInt(6, item.getMenuItemId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateAvailability(int menuItemId, boolean available) {
        String sql = "UPDATE menu_items SET availability = ? WHERE menu_item_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBoolean(1, available);
            statement.setInt(2, menuItemId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMenuItem(int menuItemId) {
        String sql = "DELETE FROM menu_items WHERE menu_item_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, menuItemId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private MenuItem mapResultSetToMenuItem(ResultSet resultSet) throws SQLException {
        MenuItem item = new MenuItem();
        item.setMenuItemId(resultSet.getInt("menu_item_id"));
        item.setRestaurantId(resultSet.getInt("restaurant_id"));
        item.setItemName(resultSet.getString("item_name"));
        item.setDescription(resultSet.getString("description"));
        item.setPrice(resultSet.getBigDecimal("price"));
        item.setCategory(resultSet.getString("category"));
        item.setAvailability(resultSet.getBoolean("availability"));
        return item;
    }
}
