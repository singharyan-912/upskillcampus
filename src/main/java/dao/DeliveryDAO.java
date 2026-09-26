package dao;

import config.DatabaseConnection;
import model.Delivery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DeliveryDAO {

    public boolean createDelivery(Delivery delivery) {
        String sql = "INSERT INTO deliveries (order_id, delivery_partner_id, delivery_status) VALUES (?, ?, ?)";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, delivery.getOrderId());
            statement.setInt(2, delivery.getDeliveryPartnerId());
            statement.setString(3, delivery.getDeliveryStatus());

            int rows = statement.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        delivery.setDeliveryId(keys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Delivery getDeliveryByOrderId(int orderId) {
        String sql = "SELECT * FROM deliveries WHERE order_id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToDelivery(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateDeliveryStatus(int deliveryId, String newStatus) {
        String sql = "UPDATE deliveries SET delivery_status = ?";
        if ("PICKED_UP".equals(newStatus)) {
            sql += ", pickup_time = CURRENT_TIMESTAMP";
        } else if ("DELIVERED".equals(newStatus)) {
            sql += ", delivered_time = CURRENT_TIMESTAMP";
        }
        sql += " WHERE delivery_id = ?";
        
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, newStatus);
            statement.setInt(2, deliveryId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Delivery mapResultSetToDelivery(ResultSet rs) throws SQLException {
        Delivery delivery = new Delivery();
        delivery.setDeliveryId(rs.getInt("delivery_id"));
        delivery.setOrderId(rs.getInt("order_id"));
        delivery.setDeliveryPartnerId(rs.getInt("delivery_partner_id"));
        delivery.setDeliveryStatus(rs.getString("delivery_status"));
        delivery.setAssignedTime(rs.getTimestamp("assigned_time"));
        delivery.setPickupTime(rs.getTimestamp("pickup_time"));
        delivery.setDeliveredTime(rs.getTimestamp("delivered_time"));
        return delivery;
    }
}
