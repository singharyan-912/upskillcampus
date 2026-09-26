package controller;

import com.google.gson.Gson;
import model.Payment;
import service.PaymentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * PaymentServlet — handles payment processing for orders.
 *
 * Endpoints:
 *   POST /api/payments/process          → process COD or ONLINE payment
 *   GET  /api/payments?orderId={id}     → fetch payment records for an order
 *
 * Session attributes used (set by AuthServlet):
 *   "userId"   (Integer)
 *   "userRole" (String)
 */
@WebServlet("/api/payments/*")
public class PaymentServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();
    private final Gson           gson           = new Gson();

    // ─────────────── GET /api/payments?orderId={id} ───────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null || orderIdParam.isBlank()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "orderId query parameter is required.");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdParam.trim());
            List<Payment> payments = paymentService.getPaymentsByOrderId(userId, orderId);
            if (payments != null) {
                JsonUtil.sendSuccess(resp, "Payments retrieved.", payments);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Access denied or order not found.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid orderId format.");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.");
        }
    }

    // ─────────────── POST /api/payments/process ───────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;
        String role = getAuthRole(req);

        String pathInfo = req.getPathInfo();

        if ("/process".equals(pathInfo)) {
            if (!"CUSTOMER".equals(role)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Only customers can process payments.");
                return;
            }

            try {
                PaymentRequest paymentReq = gson.fromJson(req.getReader(), PaymentRequest.class);
                if (paymentReq == null || paymentReq.method == null || paymentReq.orderId <= 0) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "orderId and method (COD or ONLINE) are required.");
                    return;
                }

                // Validate method
                String method = paymentReq.method.trim().toUpperCase();
                if (!"COD".equals(method) && !"ONLINE".equals(method)) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid payment method. Use COD or ONLINE.");
                    return;
                }

                // simulateSuccess: frontend sends true for demo success, false for demo failure.
                // Defaults to true (backward compatible) if not provided.
                boolean simulateSuccess = paymentReq.simulateSuccess; // defaults to false in Java if not in JSON
                if ("COD".equals(method)) {
                    simulateSuccess = true; // COD always succeeds (status = PENDING)
                } else if (!paymentReq.simulateSuccessProvided) {
                    simulateSuccess = true; // default ONLINE to success if flag omitted
                }

                Payment payment = paymentService.processPayment(
                        userId,
                        paymentReq.orderId,
                        method,
                        simulateSuccess
                );

                if (payment != null) {
                    String msg;
                    if ("COD".equals(method)) {
                        msg = "Cash on Delivery payment recorded. Please pay upon delivery.";
                    } else if ("SUCCESS".equals(payment.getPaymentStatus())) {
                        msg = "Payment successful! Transaction: " + payment.getTransactionRef();
                    } else {
                        msg = "Payment simulation failed. Transaction: " + payment.getTransactionRef();
                    }
                    JsonUtil.sendSuccess(resp, msg, payment);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Could not process payment. The order may not exist, " +
                            "may not belong to you, or has already been paid.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred.");
            }

        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
        }
    }

    // ─────────────── HELPERS ───────────────

    private Integer getAuthUserId(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        return (Integer) session.getAttribute("userId");
    }

    private String getAuthRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return "";
        Object role = session.getAttribute("userRole");
        return role != null ? role.toString() : "";
    }

    // ─────────────── DTO ───────────────

    /**
     * JSON body for POST /api/payments/process.
     * {
     *   "orderId": 42,
     *   "method": "ONLINE",          // "COD" or "ONLINE"
     *   "simulateSuccess": true,     // optional; true = success, false = failure (ONLINE only)
     *   "simulateSuccessProvided": true  // frontend must send this to distinguish from default
     * }
     */
    private static class PaymentRequest {
        int     orderId;
        String  method;
        boolean simulateSuccess;         // Gson defaults this to false if absent
        boolean simulateSuccessProvided; // set to true by frontend so we know it was intentional
    }
}
