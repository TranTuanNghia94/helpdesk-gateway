package com.it.gateway.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.it.gateway.service.Security.CustomUserDetailsService;
import com.it.gateway.service.Security.JwtService;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Skip JWT processing for public endpoints
        String requestURI = request.getRequestURI().replace("/api/v1", "");
        if (requestURI.equals("/users/login") || 
            requestURI.startsWith("/actuator/") || 
            requestURI.startsWith("/v3/api-docs/") || 
            requestURI.startsWith("/swagger-ui/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from Bearer header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No token provided");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);

        // If we got a username and no authentication is set
        if (username != null && jwtService.isTokenValid(jwt, username)) {

            if (jwtService.isTokenExpired(jwt)) {
                log.warn("Token expired - user: {}", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            if (userDetails == null) {
                log.warn("User not found - user: {}", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
                    
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } else {
            log.warn("Invalid token or token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
