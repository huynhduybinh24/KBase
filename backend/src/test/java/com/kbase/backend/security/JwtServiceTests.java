package com.kbase.backend.security;

import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {

    private static final String SECRET =
            "bXktdmVyeS1sb25nLWRldmVsb3BtZW50LW9ubHktand0LXNlY3JldC1rZXktMzItYnl0ZXM=";
    private static final String OTHER_SECRET =
            "YW5vdGhlci12ZXJ5LWxvbmctand0LXNlY3JldC1rZXktZm9yLXRlc3Rpbmctb25seQ==";

    @Test
    void generatesSignedTokenForUser() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        User user = new User(
                "person@example.com",
                "unused-password-hash",
                Set.of(Role.USER)
        );

        String token = jwtService.generateToken(user);

        assertEquals("person@example.com", jwtService.extractSubject(token));
        assertTrue(jwtService.isValid(token, "person@example.com"));
        assertFalse(jwtService.isValid(token, "someone@example.com"));
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService issuer = new JwtService(SECRET, 3_600_000L);
        JwtService verifier = new JwtService(OTHER_SECRET, 3_600_000L);
        User user = new User(
                "person@example.com",
                "unused-password-hash",
                Set.of(Role.ADMIN, Role.OWNER, Role.USER)
        );

        String token = issuer.generateToken(user);

        assertThrows(JwtException.class, () -> verifier.extractSubject(token));
    }

    @Test
    void rejectsNonPositiveExpirationConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 0));
    }
}
