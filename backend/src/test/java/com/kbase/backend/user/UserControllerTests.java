package com.kbase.backend.user;

import com.kbase.backend.auth.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTests {

    @Test
    void derivesCurrentUserFromSpringSecurityPrincipal() {
        UserService userService = mock(UserService.class);
        UserDetails principal = mock(UserDetails.class);
        UserResponse expected = new UserResponse(
                UUID.randomUUID(),
                "person@example.com",
                Set.of("USER")
        );
        when(principal.getUsername()).thenReturn("person@example.com");
        when(userService.getCurrentUser("person@example.com")).thenReturn(expected);

        UserResponse response = new UserController(userService).getCurrentUser(principal);

        assertEquals(expected, response);
        verify(userService).getCurrentUser("person@example.com");
    }
}
