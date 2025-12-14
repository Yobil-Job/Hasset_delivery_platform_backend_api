package com.kuru.delivery.common.util;

import com.kuru.delivery.user.model.Role;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for validating user roles and ownership.
 * Provides additional security layer beyond @PreAuthorize annotations.
 */
public final class RoleValidationUtil {

    private RoleValidationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that the current authenticated user has the ADMIN role.
     * Throws SecurityException if user is not an admin.
     *
     * @param userRepository repository to fetch user from database
     * @throws SecurityException if user is not an admin
     */
    public static void validateAdminRole(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException("Access denied: Admin role required");
        }
    }

    /**
     * Validates that the current authenticated user has the specified role.
     *
     * @param requiredRole the role required
     * @param userRepository repository to fetch user from database
     * @throws SecurityException if user doesn't have the required role
     */
    public static void validateRole(Role requiredRole, UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        if (user.getRole() != requiredRole) {
            throw new SecurityException("Access denied: " + requiredRole.name() + " role required");
        }
    }

    /**
     * Gets the current authenticated user's ID.
     *
     * @param userRepository repository to fetch user from database
     * @return the user ID
     * @throws SecurityException if user is not authenticated
     */
    public static Long getCurrentUserId(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }

    /**
     * Gets the current authenticated user.
     *
     * @param userRepository repository to fetch user from database
     * @return the user
     * @throws SecurityException if user is not authenticated
     */
    public static User getCurrentUser(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String email = userDetails.getUsername();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));
    }
}

