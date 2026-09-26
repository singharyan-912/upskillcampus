package controller;

import com.google.gson.Gson;
import model.Review;
import service.ReviewService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReviewServlet — handles restaurant reviews and ratings.
 *
 * Endpoints:
 *   POST /api/reviews/submit                       → submit a review (CUSTOMER only)
 *   GET  /api/reviews?restaurantId={id}            → list reviews for a restaurant
 *   GET  /api/reviews/rating?restaurantId={id}     → average rating for a restaurant
 *
 * All validation (ownership, DELIVERED status, one-per-order, rating range)
 * is performed entirely in ReviewService — the servlet stays thin.
 */
@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {

    private final ReviewService reviewService = new ReviewService();
    private final Gson          gson          = new Gson();

    // ─────────────── GET ───────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // null | "/" | "/rating"

        try {
            // GET /api/reviews/rating?restaurantId={id}  → average rating
            if ("/rating".equals(pathInfo)) {
                String restIdParam = req.getParameter("restaurantId");
                if (restIdParam == null || restIdParam.isBlank()) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "restaurantId query parameter is required.");
                    return;
                }
                int restaurantId = Integer.parseInt(restIdParam.trim());
                double avg = reviewService.getAverageRating(restaurantId);

                Map<String, Object> data = new HashMap<>();
                data.put("restaurantId",   restaurantId);
                data.put("averageRating",  avg);
                JsonUtil.sendSuccess(resp, "Average rating retrieved.", data);
                return;
            }

            // GET /api/reviews?restaurantId={id}  → list of reviews
            String restIdParam = req.getParameter("restaurantId");
            if (restIdParam == null || restIdParam.isBlank()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "restaurantId query parameter is required.");
                return;
            }
            int restaurantId = Integer.parseInt(restIdParam.trim());
            List<Review> reviews = reviewService.getReviewsForRestaurant(restaurantId);
            JsonUtil.sendSuccess(resp, "Reviews retrieved.", reviews);

        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid restaurantId format.");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.");
        }
    }

    // ─────────────── POST /api/reviews/submit ───────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Must be logged in
        Integer userId = getAuthUserId(req, resp);
        if (userId == null) return;

        String role = getAuthRole(req);
        if (!"CUSTOMER".equals(role)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Only customers can submit reviews.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (!"/submit".equals(pathInfo)) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint.");
            return;
        }

        try {
            ReviewRequest reviewReq = gson.fromJson(req.getReader(), ReviewRequest.class);
            if (reviewReq == null || reviewReq.orderId <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "orderId is required.");
                return;
            }
            if (reviewReq.rating < 1 || reviewReq.rating > 5) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Rating must be between 1 and 5.");
                return;
            }

            // All business validation happens inside ReviewService
            boolean success = reviewService.submitReview(
                    userId,
                    reviewReq.orderId,
                    reviewReq.rating,
                    reviewReq.comment != null ? reviewReq.comment.trim() : ""
            );

            if (success) {
                JsonUtil.sendSuccess(resp, "Review submitted successfully! Thank you.", null);
            } else {
                // Service prints the specific reason; surface a helpful message
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Could not submit review. Possible reasons: order not delivered yet, " +
                        "order does not belong to you, or you have already reviewed this order.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.");
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
     * JSON body for POST /api/reviews/submit:
     * {
     *   "orderId": 42,
     *   "rating": 4,
     *   "comment": "Great food!"
     * }
     */
    private static class ReviewRequest {
        int    orderId;
        int    rating;
        String comment;
    }
}
