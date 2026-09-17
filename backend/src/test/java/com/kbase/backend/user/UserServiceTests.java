package com.kbase.backend.user;

import com.kbase.backend.auth.dto.UserResponse;
import com.kbase.backend.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTests {

    @Test
    void returnsUserMatchingAuthenticatedEmail() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User(
                "person@example.com",
                "$2a$10$unusedHashForCurrentUserTest0000000000000000000000000",
                Set.of(Role.USER)
        );
        when(repository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response = new UserService(repository)
                .getCurrentUser("person@example.com");

        assertEquals("person@example.com", response.email());
        assertEquals(Set.of("USER"), response.roles());
        verify(repository).findByEmailIgnoreCase("person@example.com");
    }

    @Test
    void rejectsAuthenticatedIdentityMissingFromDatabase() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("deleted@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> new UserService(repository).getCurrentUser("deleted@example.com")
        );
    }
}
