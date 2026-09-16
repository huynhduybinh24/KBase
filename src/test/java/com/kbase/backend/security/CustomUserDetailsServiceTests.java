package com.kbase.backend.security;

import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTests {

    @Test
    void mapsEveryRoleToSpringSecurityAuthority() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User(
                "person@example.com",
                "$2a$10$unusedHashForAuthorityTest000000000000000000000000000",
                Set.of(Role.ADMIN, Role.OWNER, Role.USER)
        );
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository)
                .loadUserByUsername("person@example.com");

        Set<String> authorities = details.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_OWNER", "ROLE_USER"), authorities);
    }
}
