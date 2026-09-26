package service;

import dao.DeliveryDAO;
import dao.OrderDAO;
import dao.UserDAO;
import model.Delivery;
import model.Order;
import model.User;

public class DeliveryService {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    public boolean assignDelivery(int partnerId, int orderId) {
        // Validate user is a delivery partner
        User partner = userDAO.getUserById(partnerId);
        if (partner == null || !"DELIVERY_PARTNER".equals(partner.getRole())) {
            System.out.println("Access denied. Only DELIVERY_PARTNER can assign deliveries.");
            return false;
        }

        // Validate order is READY
        Order order = orderDAO.getOrderById(orderId);
        if (order == null || !"READY".equals(order.getStatus())) {
            System.out.println("Order is not READY for delivery.");
            return false;
        }

        // Validate not already assigned
        if (deliveryDAO.getDeliveryByOrderId(orderId) != null) {
            System.out.println("Order is already assigned to a delivery partner.");
            return false;
        }

        Delivery delivery = new Delivery(orderId, partnerId);
        boolean success = deliveryDAO.createDelivery(delivery);
        if (success) {
            System.out.println("Order " + orderId + " successfully assigned to partner " + partnerId);
        } else {
            System.out.println("Failed to assign delivery.");
        }
        return success;
    }

    public boolean updateDeliveryStatus(int partnerId, int orderId, String newStatus) {
        Delivery delivery = deliveryDAO.getDeliveryByOrderId(orderId);
        if (delivery == null) {
            System.out.println("No delivery found for this order.");
            return false;
        }
        if (delivery.getDeliveryPartnerId() != partnerId) {
            System.out.println("Access denied. You are not assigned to this delivery.");
            return false;
        }

        String currentStatus = delivery.getDeliveryStatus();

        if ("DELIVERED".equals(currentStatus)) {
            System.out.println("Delivery is already completed.");
            return false;
        }

        boolean validTransition = false;
        switch (newStatus) {
            case "PICKED_UP":
                if ("ASSIGNED".equals(currentStatus)) validTransition = true;
                break;
            case "OUT_FOR_DELIVERY":
                if ("PICKED_UP".equals(currentStatus)) validTransition = true;
                break;
            case "DELIVERED":
                if ("OUT_FOR_DELIVERY".equals(currentStatus)) validTransition = true;
                break;
        }

        if (!validTransition) {
            System.out.println("Invalid delivery status transition from " + currentStatus + " to " + newStatus);
            return false;
        }

        boolean success = deliveryDAO.updateDeliveryStatus(delivery.getDeliveryId(), newStatus);
        if (success) {
            System.out.println("Delivery status updated to: " + newStatus);
            if ("DELIVERED".equals(newStatus)) {
                // Sync status with orders table
                orderDAO.updateOrderStatus(orderId, "DELIVERED");
                System.out.println("Order status successfully updated to DELIVERED.");
            }
        } else {
            System.out.println("Failed to update delivery status.");
        }
        return success;
    }
    
    public Delivery getDeliveryDetails(int orderId) {
        return deliveryDAO.getDeliveryByOrderId(orderId);
    }
}
