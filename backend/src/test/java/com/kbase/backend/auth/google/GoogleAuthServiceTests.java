package com.kbase.backend.auth.google;

import com.kbase.backend.auth.AuthService;
import com.kbase.backend.auth.dto.AuthResponse;
import com.kbase.backend.exception.GoogleAuthenticationException;
import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTests {
    @Mock GoogleIdentityVerifier verifier;
    @Mock UserExternalIdentityRepository identities;
    @Mock UserRepository users;
    @Mock UserProfileService profiles;
    @Mock AuthService authService;
    GoogleAuthService service;

    @BeforeEach void setup() {
        service = new GoogleAuthService(verifier, identities, users, profiles, authService);
        lenient().when(identities.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void createsNewGoogleUserProfileRoleAndExternalIdentity() {
        VerifiedGoogleIdentity google = verified("subject-1", " Person@Example.com ", "Person Example");
        when(verifier.verify("credential")).thenReturn(google);
        when(identities.findByProviderAndProviderSubject(ExternalIdentityProvider.GOOGLE, "subject-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(authService.response(any())).thenReturn(mock(AuthResponse.class));

        service.authenticate("credential");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(user.capture());
        assertNull(user.getValue().passwordHash());
        assertEquals(Set.of(Role.USER), user.getValue().getRoles());
        verify(profiles).createForRegistration(user.getValue(), "Person Example");
        verify(identities).saveAndFlush(any(UserExternalIdentity.class));
        verify(authService).response(user.getValue());
    }

    @Test void reusesSameUserForSecondLoginBySubject() {
        User existing = new User("person@example.com", null, Set.of(Role.USER));
        UserExternalIdentity linked = new UserExternalIdentity(existing, ExternalIdentityProvider.GOOGLE, "subject-1", "person@example.com");
        when(verifier.verify("credential")).thenReturn(verified("subject-1", "person@example.com", "Person"));
        when(identities.findByProviderAndProviderSubject(ExternalIdentityProvider.GOOGLE, "subject-1")).thenReturn(Optional.of(linked));
        when(authService.response(existing)).thenReturn(mock(AuthResponse.class));
        service.authenticate("credential");
        verifyNoInteractions(users, profiles);
        verify(identities, never()).saveAndFlush(any());
    }

    @Test void linksExistingPasswordAccountWithoutChangingPasswordOrProfile() {
        User existing = new User("person@example.com", "original-hash", Set.of(Role.USER));
        when(verifier.verify("credential")).thenReturn(verified("subject-1", "PERSON@example.com", "Different Name"));
        when(identities.findByProviderAndProviderSubject(any(), any())).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(existing));
        when(authService.response(existing)).thenReturn(mock(AuthResponse.class));
        service.authenticate("credential");
        assertEquals("original-hash", existing.passwordHash());
        verifyNoInteractions(profiles);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void usesEmailLocalPartWhenGoogleNameMissing() {
        when(verifier.verify("credential")).thenReturn(verified("subject-1", "abc@example.com", null));
        when(identities.findByProviderAndProviderSubject(any(), any())).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("abc@example.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(authService.response(any())).thenReturn(mock(AuthResponse.class));
        service.authenticate("credential");
        verify(profiles).createForRegistration(any(), eq("abc"));
    }

    @Test void rejectsUnverifiedEmail() {
        when(verifier.verify("credential")).thenReturn(new VerifiedGoogleIdentity("subject", "a@b.com", false, null, null));
        assertThrows(GoogleAuthenticationException.class, () -> service.authenticate("credential"));
        verifyNoInteractions(users, identities, profiles, authService);
    }

    @Test void propagatesSafeVerificationFailureForInvalidExpiredOrWrongAudienceTokens() {
        when(verifier.verify(any())).thenThrow(new GoogleAuthenticationException());
        GoogleAuthenticationException error = assertThrows(GoogleAuthenticationException.class, () -> service.authenticate("bad-token"));
        assertEquals("Your Google account could not be verified", error.getMessage());
    }

    private VerifiedGoogleIdentity verified(String subject, String email, String name) {
        return new VerifiedGoogleIdentity(subject, email, true, name, null);
    }
}
