package service;

import dao.UserDAO;
import model.User;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public boolean registerUser(User user) {

        // Basic validation

        if (user.getName() == null ||
                user.getName().isBlank()) {

            System.out.println("Name cannot be empty.");
            return false;
        }

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {

            System.out.println("Email cannot be empty.");
            return false;
        }

        if (user.getPassword() == null ||
                user.getPassword().length() < 6) {

            System.out.println(
                    "Password must contain at least 6 characters."
            );

            return false;
        }

        if (user.getRole() == null ||
                user.getRole().isBlank()) {

            System.out.println("Role is required.");
            return false;
        }

        if (userDAO.isEmailExists(user.getEmail())) {
            System.out.println("Email is already registered.");
            return false;
        }

        return userDAO.registerUser(user);
    }

    public User loginUser(String email, String password) {

        // Validate input
        if (email == null || email.isBlank()) {
            System.out.println("Email cannot be empty.");
            return null;
        }

        if (password == null || password.isBlank()) {
            System.out.println("Password cannot be empty.");
            return null;
        }

        // Search users table
        User user = userDAO.getUserByEmail(email);

        // Check credentials
        if (user == null) {
            System.out.println("User not found.");
            return null;
        }

        if (!user.getPassword().equals(password)) {
            System.out.println("Invalid password.");
            return null;
        }

        // Do not expose unnecessary password data after login
        user.setPassword(null);

        return user;
    }
}
