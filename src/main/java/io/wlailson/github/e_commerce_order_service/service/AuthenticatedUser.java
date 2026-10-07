package io.wlailson.github.e_commerce_order_service.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    private Jwt getJwt() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return (Jwt) authentication.getPrincipal();
    }

    public Long getUserId() {
        return getJwt().getClaim("userId");
    }
}