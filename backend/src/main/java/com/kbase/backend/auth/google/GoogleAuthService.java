package com.kbase.backend.auth.google;

import com.kbase.backend.auth.AuthService;
import com.kbase.backend.auth.dto.AuthResponse;
import com.kbase.backend.exception.GoogleAuthenticationException;
import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.user.profile.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class GoogleAuthService {
    private final GoogleIdentityVerifier verifier;
    private final UserExternalIdentityRepository identities;
    private final UserRepository users;
    private final UserProfileService profiles;
    private final AuthService authService;

    public GoogleAuthService(GoogleIdentityVerifier verifier, UserExternalIdentityRepository identities,
                             UserRepository users, UserProfileService profiles, AuthService authService) {
        this.verifier = verifier; this.identities = identities; this.users = users;
        this.profiles = profiles; this.authService = authService;
    }

    @Transactional
    public AuthResponse authenticate(String credential) {
        VerifiedGoogleIdentity google = verifier.verify(credential);
        if (!google.emailVerified()) throw new GoogleAuthenticationException();
        String email = google.email().trim().toLowerCase(Locale.ROOT);

        User user = identities.findByProviderAndProviderSubject(ExternalIdentityProvider.GOOGLE, google.subject())
                .map(UserExternalIdentity::getUser)
                .orElseGet(() -> linkOrCreate(google, email));
        return authService.response(user);
    }

    private User linkOrCreate(VerifiedGoogleIdentity google, String email) {
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = users.saveAndFlush(new User(email, null, Set.of(Role.USER)));
            String name = normalizedName(google.name(), email);
            profiles.createForRegistration(user, name);
        }
        identities.saveAndFlush(new UserExternalIdentity(user, ExternalIdentityProvider.GOOGLE,
                google.subject(), email));
        return user;
    }

    private String normalizedName(String name, String email) {
        if (name != null) {
            String normalized = name.trim().replaceAll("\\s+", " ");
            if (!normalized.isEmpty()) return normalized.substring(0, Math.min(120, normalized.length()));
        }
        return email.substring(0, email.indexOf('@'));
    }
}
