package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared JSON helper utility for all Servlets.
 * Keeps Servlet code clean and response format consistent.
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    /**
     * Write any object as a JSON HTTP response.
     */
    public static void sendJson(HttpServletResponse response, int statusCode, Object data)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Allow the browser to call our API from the same origin
        response.setHeader("Cache-Control", "no-cache");
        PrintWriter out = response.getWriter();
        out.print(GSON.toJson(data));
        out.flush();
    }

    /**
     * Convenience: success response with optional data payload.
     */
    public static void sendSuccess(HttpServletResponse response, String message, Object data)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        sendJson(response, HttpServletResponse.SC_OK, body);
    }

    /**
     * Convenience: error response.
     */
    public static void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        sendJson(response, status, body);
    }

    /**
     * Parse a numeric path segment at the given position from a URI.
     * e.g. /api/restaurants/42 → position 3 → 42
     * Returns -1 if not present or not numeric.
     */
    public static int extractIdFromPath(String requestURI, int position) {
        String[] parts = requestURI.split("/");
        if (parts.length > position) {
            try {
                return Integer.parseInt(parts[position]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public static Gson getGson() {
        return GSON;
    }
}
