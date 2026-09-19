package com.kbase.backend.auth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kbase.backend.exception.GoogleAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleIdTokenIdentityVerifier implements GoogleIdentityVerifier {
    private final String clientId;

    public GoogleIdTokenIdentityVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public VerifiedGoogleIdentity verify(String credential) {
        if (clientId == null || clientId.isBlank()) throw new GoogleAuthenticationException();
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) throw new GoogleAuthenticationException();
            GoogleIdToken.Payload payload = token.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            boolean verified = Boolean.TRUE.equals(payload.getEmailVerified());
            if (subject == null || subject.isBlank() || email == null || email.isBlank() || !verified) {
                throw new GoogleAuthenticationException();
            }
            return new VerifiedGoogleIdentity(subject, email, true,
                    stringClaim(payload, "name"), stringClaim(payload, "picture"));
        } catch (GoogleAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GoogleAuthenticationException();
        }
    }

    private String stringClaim(GoogleIdToken.Payload payload, String key) {
        Object value = payload.get(key);
        return value instanceof String text ? text : null;
    }
}
