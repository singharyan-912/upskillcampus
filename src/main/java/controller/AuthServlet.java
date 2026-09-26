package controller;

import com.google.gson.JsonObject;
import dao.UserDAO;
import model.User;
import service.UserService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuthServlet — handles user registration, login, logout, and session check.
 *
 * Endpoints:
 *   POST /api/auth/register  → register a new user
 *   POST /api/auth/login     → login, creates session, returns role
 *   POST /api/auth/logout    → invalidates session
 *   GET  /api/auth/me        → returns current session user info
 *
 * Responsibility: Read input → call UserService → return JSON.
 * Zero business logic lives here.
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserService userService = new UserService();
    private final UserDAO userDAO = new UserDAO();

    // ─────────────── GET /api/auth/me ───────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String path = request.getPathInfo(); // e.g. "/me"

        if ("/me".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                JsonUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
                return;
            }

            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("userId", session.getAttribute("userId"));
            userData.put("name",   session.getAttribute("userName"));
            userData.put("email",  session.getAttribute("userEmail"));
            userData.put("role",   session.getAttribute("userRole"));

            JsonUtil.sendSuccess(response, "Authenticated.", userData);

        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─────────────── POST /api/auth/register | /api/auth/login | /api/auth/logout ───────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String path = request.getPathInfo(); // "/register" | "/login" | "/logout"

        if ("/register".equals(path)) {
            handleRegister(request, response);
        } else if ("/login".equals(path)) {
            handleLogin(request, response);
        } else if ("/logout".equals(path)) {
            handleLogout(request, response);
        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    // ─────────────── REGISTER ───────────────
    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        JsonObject body = parseJsonBody(request);
        if (body == null) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body.");
            return;
        }

        String name     = getStringSafe(body, "name");
        String email    = getStringSafe(body, "email");
        String password = getStringSafe(body, "password");
        String phone    = getStringSafe(body, "phone");
        String role     = getStringSafe(body, "role");

        // Basic presence check before calling service
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Name, email, password, and role are required.");
            return;
        }

        // Check duplicate email first so we can return a specific message
        if (userDAO.isEmailExists(email)) {
            JsonUtil.sendError(response, HttpServletResponse.SC_CONFLICT,
                    "Email is already registered. Please login or use a different email.");
            return;
        }

        // Validate allowed roles
        if (!isValidRole(role)) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid role. Allowed: CUSTOMER, RESTAURANT, DELIVERY_PARTNER, ADMIN.");
            return;
        }

        if (password.length() < 6) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Registration failed. Password must be at least 6 characters.");
            return;
        }

        User user = new User(name, email, password, phone, role.toUpperCase());
        boolean success = userService.registerUser(user);

        if (success) {
            JsonUtil.sendSuccess(response, "Registration successful! Please login.", null);
        } else {
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Registration failed. Our servers might be experiencing an issue. Please try again.");
        }
    }

    // ─────────────── LOGIN ───────────────
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        JsonObject body = parseJsonBody(request);
        if (body == null) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body.");
            return;
        }

        String email    = getStringSafe(body, "email");
        String password = getStringSafe(body, "password");

        if (email.isEmpty() || password.isEmpty()) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Email and password are required.");
            return;
        }

        // UserService.loginUser() already sets password=null on success
        User user = userService.loginUser(email, password);

        if (user == null) {
            JsonUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid email or password.");
            return;
        }

        // Store essential info in session (never store password)
        HttpSession session = request.getSession(true);
        session.setAttribute("userId",    user.getUserId());
        session.setAttribute("userName",  user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userRole",  user.getRole());
        session.setMaxInactiveInterval(60 * 60); // 1 hour

        // Build response payload — password is already null in the User object
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("name",   user.getName());
        data.put("email",  user.getEmail());
        data.put("role",   user.getRole());

        JsonUtil.sendSuccess(response, "Login successful.", data);
    }

    // ─────────────── LOGOUT ───────────────
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccess(response, "Logged out successfully.", null);
    }

    // ─────────────── HELPERS ───────────────

    /** Read the full request body and parse as a Gson JsonObject. */
    private JsonObject parseJsonBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return JsonUtil.getGson().fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString().trim();
        }
        return "";
    }

    private boolean isValidRole(String role) {
        return role.equalsIgnoreCase("CUSTOMER")
                || role.equalsIgnoreCase("RESTAURANT")
                || role.equalsIgnoreCase("DELIVERY_PARTNER")
                || role.equalsIgnoreCase("ADMIN");
    }
}
