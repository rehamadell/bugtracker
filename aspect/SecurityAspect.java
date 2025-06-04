package com.example.signupauth.aspect;

import com.example.signupauth.model.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecurityAspect {

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            if (user.getRole() == requiresRole.value()) {
                return joinPoint.proceed();
            }
        }
        throw new SecurityException("Access denied. Required role: " + requiresRole.value());
    }

    @Around("@annotation(requiresOwnership)")
    public Object checkOwnership(ProceedingJoinPoint joinPoint, RequiresOwnership requiresOwnership) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            Object[] args = joinPoint.getArgs();
            
            // Get the bug ID from method arguments
            Long bugId = null;
            for (Object arg : args) {
                if (arg instanceof Long) {
                    bugId = (Long) arg;
                    break;
                }
            }
            
            if (bugId != null) {
                // Check if the user is either the creator or the assigned developer
                if (isUserAuthorized(user, bugId)) {
                    return joinPoint.proceed();
                }
            }
        }
        throw new SecurityException("Access denied. You don't have permission to access this resource.");
    }

    private boolean isUserAuthorized(User user, Long bugId) {
        // Implement your authorization logic here
        // Check if user is creator or assigned developer
        return true; // Placeholder implementation
    }
} 