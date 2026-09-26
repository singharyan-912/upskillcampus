package service;

import dao.OrderDAO;
import dao.PaymentDAO;
import model.Order;
import model.Payment;

import java.util.List;
import java.util.UUID;

public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public Payment processPayment(int userId, int orderId, String method, boolean simulateOnlineSuccess) {
        Order order = orderDAO.getOrderById(orderId);

        if (order == null) {
            System.out.println("Order not found.");
            return null;
        }

        if (order.getUserId() != userId) {
            System.out.println("Access denied. You do not own this order.");
            return null;
        }

        if (paymentDAO.hasSuccessfulPayment(orderId)) {
            System.out.println("Order has already been paid successfully.");
            return null;
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(order.getTotalAmount());
        
        if ("COD".equalsIgnoreCase(method)) {
            payment.setPaymentMethod("COD");
            payment.setPaymentStatus("PENDING");
            payment.setTransactionRef(null);
            
            Payment created = paymentDAO.createPayment(payment);
            if (created != null) {
                System.out.println("COD Payment recorded as PENDING.");
            }
            return created;
        } else if ("ONLINE".equalsIgnoreCase(method)) {
            payment.setPaymentMethod("ONLINE");
            String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.setTransactionRef(transactionRef);

            if (simulateOnlineSuccess) {
                payment.setPaymentStatus("SUCCESS");
            } else {
                payment.setPaymentStatus("FAILED");
            }

            Payment created = paymentDAO.createPayment(payment);

            if (created != null) {
                if (simulateOnlineSuccess) {
                    System.out.println("ONLINE Payment SUCCESS! TXN: " + transactionRef);
                    // Automatically update order status to CONFIRMED on success
                    orderDAO.updateOrderStatus(orderId, "CONFIRMED");
                    System.out.println("Order status automatically updated to CONFIRMED.");
                } else {
                    System.out.println("ONLINE Payment FAILED. TXN: " + transactionRef);
                }
            }
            return created;
        } else {
            System.out.println("Invalid payment method.");
            return null;
        }
    }

    public List<Payment> getPaymentsByOrderId(int userId, int orderId) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null || order.getUserId() != userId) {
            System.out.println("Access denied or order not found.");
            return null;
        }
        return paymentDAO.getPaymentsByOrderId(orderId);
    }
}
