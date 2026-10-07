package io.wlailson.github.e_commerce_order_service.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticatedUserTest {

    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetUserId {

        @Test
        void readsUserIdFromAuthenticatedJwt() {
            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .claim("userId", 42L)
                    .build();
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThat(authenticatedUser.getUserId()).isEqualTo(42L);
        }
    }
}
