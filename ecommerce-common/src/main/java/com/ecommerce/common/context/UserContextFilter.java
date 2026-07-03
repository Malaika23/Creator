package com.ecommerce.common.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-User-Id");
        String email = httpRequest.getHeader("X-User-Email");
        String role = httpRequest.getHeader("X-User-Role");

        if (userId != null || email != null || role != null) {
            UserContext context = new UserContext();
            context.setUserId(userId);
            context.setEmail(email);
            context.setRole(role);
            UserContext.setCurrentContext(context);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
